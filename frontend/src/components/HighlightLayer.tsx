import { useMemo } from 'react'
import type { PageViewport } from 'pdfjs-dist'
import type { HighlightRect } from '../api/types'
import { bboxToCssRect } from '../lib/highlight'

interface HighlightLayerProps {
  /** Rects belonging to this page only. */
  rects: HighlightRect[]
  /** The page's viewport at scale 1. */
  viewport: PageViewport
  scale: number
}

export function HighlightLayer({ rects, viewport, scale }: HighlightLayerProps) {
  const scaled = useMemo(() => viewport.clone({ scale }), [viewport, scale])

  return (
    <div className="highlight-layer">
      {rects.map((rect, index) => {
        const box = bboxToCssRect(rect.bbox, scaled)

        return (
          <div
            key={index}
            className="highlight-rect"
            style={{
              left: box.left,
              top: box.top,
              width: box.width,
              height: box.height,
            }}
          />
        )
      })}
    </div>
  )
}
