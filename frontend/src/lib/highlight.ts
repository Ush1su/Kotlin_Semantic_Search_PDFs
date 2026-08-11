import type { PageViewport } from 'pdfjs-dist'
import type { BoundingBox } from '../api/types'

export interface CssRect {
  left: number
  top: number
  width: number
  height: number
}

/**
 * The parser stores boxes as [left, bottom, right, top] in PDF user space, where
 * y grows upwards from the bottom-left corner of the page. That is exactly what
 * convertToViewportRectangle expects, and it folds in the page's scale and
 * rotation for us.
 *
 * If highlights ever come out mirrored across the page's horizontal centerline,
 * the parser is emitting top-left-origin boxes instead and this is the one place
 * that needs to flip them.
 */
export function bboxToCssRect(bbox: BoundingBox, viewport: PageViewport): CssRect {
  const [x1, y1, x2, y2] = viewport.convertToViewportRectangle([
    bbox.left,
    bbox.bottom,
    bbox.right,
    bbox.top,
  ])

  return {
    left: Math.min(x1, x2),
    top: Math.min(y1, y2),
    width: Math.abs(x2 - x1),
    height: Math.abs(y2 - y1),
  }
}
