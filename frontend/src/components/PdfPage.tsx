import { Page } from 'react-pdf'
import type { PageViewport } from 'pdfjs-dist'
import type { HighlightRect } from '../api/types'
import { HighlightLayer } from './HighlightLayer'

interface PdfPageProps {
  pageNumber: number
  scale: number
  viewport: PageViewport
  rects: HighlightRect[]
}

export function PdfPage({ pageNumber, scale, viewport, rects }: PdfPageProps) {
  return (
    <Page
      pageNumber={pageNumber}
      scale={scale}
      renderAnnotationLayer={false}
      loading=""
      error=""
      noData=""
    >
      {rects.length > 0 ? (
        <HighlightLayer rects={rects} viewport={viewport} scale={scale} />
      ) : null}
    </Page>
  )
}
