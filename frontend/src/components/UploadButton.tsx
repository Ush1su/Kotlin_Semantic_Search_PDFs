import { useRef } from 'react'

interface UploadButtonProps {
  uploading: boolean
  onUpload: (files: File[]) => void
}

export function UploadButton({ uploading, onUpload }: UploadButtonProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  return (
    <>
      <button
        type="button"
        className="upload"
        disabled={uploading}
        onClick={() => inputRef.current?.click()}
      >
        {uploading ? 'Uploading…' : '+ Upload PDF'}
      </button>

      <input
        ref={inputRef}
        type="file"
        accept="application/pdf"
        multiple
        hidden
        onChange={(event) => {
          const files = Array.from(event.target.files ?? [])

          if (files.length > 0) {
            onUpload(files)
          }

          // Allow re-picking the same file after a failed upload.
          event.target.value = ''
        }}
      />
    </>
  )
}
