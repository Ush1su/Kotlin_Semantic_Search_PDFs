import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Document } from 'react-pdf'
import type { PDFDocumentProxy, PageViewport } from 'pdfjs-dist'
import { documentFileUrl } from '../api/client'
import type { HighlightRect, SearchResult } from '../api/types'
import type { SearchState } from '../hooks/useSearch'
import { bboxToCssRect } from '../lib/highlight'
import { PdfPage } from './PdfPage'
import { SearchResults } from './SearchResults'

const PAGE_GAP = 16
const SCALE_STEPS = [0.5, 0.75, 1, 1.25, 1.5, 2, 3]
const DEFAULT_SCALE = 1.25
/** Above this distance a smooth scroll would crawl through every page in between. */
const SMOOTH_SCROLL_LIMIT_PX = 4000

/** Which results list a highlight came from, so closing that list can drop it. */
export type HighlightSource = 'global' | 'document'

export interface ActiveHighlight {
  chunkId: string
  documentId: string
  rects: HighlightRect[]
  source: HighlightSource
  /** Bumped on each activation so re-picking the same chunk scrolls again. */
  nonce: number
}

interface PdfViewerProps {
  documentId: string
  filename: string
  highlight: ActiveHighlight | null
  search: SearchState
  onSelectResult: (result: SearchResult) => void
}

export function PdfViewer({
  documentId,
  filename,
  highlight,
  search,
  onSelectResult,
}: PdfViewerProps) {
  const [numPages, setNumPages] = useState(0)
  const [viewports, setViewports] = useState<PageViewport[]>([])
  const [scale, setScale] = useState(DEFAULT_SCALE)
  const [currentPage, setCurrentPage] = useState(1)
  const [range, setRange] = useState({ start: 0, end: 0 })

  const scrollRef = useRef<HTMLDivElement>(null)
  const frameRef = useRef<number | null>(null)
  const loadTokenRef = useRef(0)

  // react-pdf reloads the file whenever this prop changes identity.
  const file = useMemo(() => ({ url: documentFileUrl(documentId) }), [documentId])

  useEffect(() => {
    loadTokenRef.current += 1
    setNumPages(0)
    setViewports([])
    setCurrentPage(1)
    setRange({ start: 0, end: 0 })
    scrollRef.current?.scrollTo({ top: 0 })
  }, [documentId])

  /**
   * Page dimensions for every page up front. They are metadata only, so this is
   * cheap, and knowing them lets unrendered pages hold their space — which keeps
   * jumping straight to a search hit accurate.
   */
  const handleLoadSuccess = useCallback(async (pdf: PDFDocumentProxy) => {
    const token = ++loadTokenRef.current
    setNumPages(pdf.numPages)

    const collected: PageViewport[] = []

    for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber++) {
      const page = await pdf.getPage(pageNumber)

      if (loadTokenRef.current !== token) {
        return
      }

      collected.push(page.getViewport({ scale: 1 }))
    }

    setViewports(collected)
  }, [])

  const layout = useMemo(() => {
    const sizes = viewports.map((viewport) => ({
      width: viewport.width * scale,
      height: viewport.height * scale,
    }))

    const offsets: number[] = []
    let top = 0

    for (const size of sizes) {
      offsets.push(top)
      top += size.height + PAGE_GAP
    }

    return {
      sizes,
      offsets,
      totalHeight: Math.max(0, top - PAGE_GAP),
      maxWidth: sizes.reduce((widest, size) => Math.max(widest, size.width), 0),
    }
  }, [viewports, scale])

  /** Keeps the mounted page window and the page indicator in step with scrolling. */
  const updateWindow = useCallback(() => {
    const element = scrollRef.current

    if (!element || layout.offsets.length === 0) {
      return
    }

    const scrollTop = element.scrollTop
    const overscan = element.clientHeight
    const windowTop = scrollTop - overscan
    const windowBottom = scrollTop + element.clientHeight + overscan

    let start = -1
    let end = 0
    let visiblePage = 1

    for (let index = 0; index < layout.offsets.length; index++) {
      const pageTop = layout.offsets[index]
      const pageBottom = pageTop + layout.sizes[index].height

      if (pageBottom >= windowTop && pageTop <= windowBottom) {
        if (start === -1) {
          start = index
        }
        end = index
      }

      if (pageTop <= scrollTop + 80) {
        visiblePage = index + 1
      }
    }

    if (start === -1) {
      start = 0
      end = 0
    }

    setRange((previous) =>
      previous.start === start && previous.end === end ? previous : { start, end },
    )
    setCurrentPage((previous) => (previous === visiblePage ? previous : visiblePage))
  }, [layout])

  useEffect(() => {
    updateWindow()
  }, [updateWindow])

  const handleScroll = useCallback(() => {
    if (frameRef.current !== null) {
      return
    }

    frameRef.current = requestAnimationFrame(() => {
      frameRef.current = null
      updateWindow()
    })
  }, [updateWindow])

  useEffect(
    () => () => {
      if (frameRef.current !== null) {
        cancelAnimationFrame(frameRef.current)
      }
    },
    [],
  )

  // Bring the highlighted passage into view once the page sizes are known.
  useEffect(() => {
    const element = scrollRef.current

    if (!element || !highlight || highlight.documentId !== documentId) {
      return
    }

    const first = highlight.rects[0]
    const index = first ? first.pageNumber - 1 : -1

    if (!first || index < 0 || index >= viewports.length) {
      return
    }

    const box = bboxToCssRect(first.bbox, viewports[index].clone({ scale }))
    const target = Math.max(0, layout.offsets[index] + box.top - element.clientHeight / 3)
    const distance = Math.abs(target - element.scrollTop)

    element.scrollTo({
      top: target,
      behavior: distance > SMOOTH_SCROLL_LIMIT_PX ? 'auto' : 'smooth',
    })
  }, [highlight, documentId, viewports, layout, scale])

  const rectsByPage = useMemo(() => {
    const map = new Map<number, HighlightRect[]>()

    if (!highlight || highlight.documentId !== documentId) {
      return map
    }

    for (const rect of highlight.rects) {
      const rects = map.get(rect.pageNumber)

      if (rects) {
        rects.push(rect)
      } else {
        map.set(rect.pageNumber, [rect])
      }
    }

    return map
  }, [highlight, documentId])

  const changeZoom = useCallback(
    (step: number) => {
      const next = SCALE_STEPS[SCALE_STEPS.indexOf(scale) + step]

      if (next === undefined) {
        return
      }

      const element = scrollRef.current
      const ratio = next / scale
      setScale(next)

      if (element) {
        const anchored = element.scrollTop * ratio
        requestAnimationFrame(() => {
          element.scrollTop = anchored
        })
      }
    },
    [scale],
  )

  return (
    <section className="viewer">
      <header className="viewer-toolbar">
        <span className="viewer-filename" title={filename}>
          {filename}
        </span>

        <span className="viewer-position">
          {numPages > 0 ? `page ${currentPage} / ${numPages}` : '—'}
        </span>

        <div className="zoom">
          <button type="button" onClick={() => changeZoom(-1)} aria-label="Zoom out">
            −
          </button>
          <span>{Math.round(scale * 100)}%</span>
          <button type="button" onClick={() => changeZoom(1)} aria-label="Zoom in">
            +
          </button>
        </div>

        <form className="viewer-search" onSubmit={(event) => event.preventDefault()}>
          <input
            type="search"
            value={search.query}
            placeholder="Search in this PDF…"
            onChange={(event) => search.setQuery(event.target.value)}
          />
        </form>
      </header>

      {search.status !== 'idle' ? (
        <div className="viewer-results">
          <div className="viewer-results-head">
            <span>In this document</span>
            <button type="button" className="link" onClick={search.clear}>
              close
            </button>
          </div>
          <SearchResults
            state={search}
            activeChunkId={highlight?.chunkId ?? null}
            onSelect={onSelectResult}
          />
        </div>
      ) : null}

      <Document
        file={file}
        className="viewer-document"
        onLoadSuccess={handleLoadSuccess}
        loading={<p className="viewer-status">Loading PDF…</p>}
        error={<p className="viewer-status error">Could not load this PDF.</p>}
      >
        <div className="viewer-scroll" ref={scrollRef} onScroll={handleScroll}>
          <div
            className="viewer-canvas"
            style={{ height: layout.totalHeight, width: layout.maxWidth }}
          >
            {viewports.map((viewport, index) => {
              const size = layout.sizes[index]
              const mounted = index >= range.start && index <= range.end

              return (
                <div
                  key={index}
                  className="page-slot"
                  style={{
                    top: layout.offsets[index],
                    width: size.width,
                    height: size.height,
                    marginLeft: -size.width / 2,
                  }}
                >
                  {mounted ? (
                    <PdfPage
                      pageNumber={index + 1}
                      scale={scale}
                      viewport={viewport}
                      rects={rectsByPage.get(index + 1) ?? []}
                    />
                  ) : null}
                </div>
              )
            })}
          </div>
        </div>
      </Document>
    </section>
  )
}
