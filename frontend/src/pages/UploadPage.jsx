import { useState, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import { documentApi } from '../services/api'
import toast from 'react-hot-toast'
import { Upload, FileText, X, ArrowLeft, CheckCircle } from 'lucide-react'

const ACCEPTED_TYPES = {
  'application/pdf': ['.pdf'],
  'image/png': ['.png'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'application/msword': ['.doc'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
}

export default function UploadPage() {
  const navigate = useNavigate()
  const [file, setFile] = useState(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)

  const onDrop = useCallback((accepted, rejected) => {
    if (rejected.length > 0) {
      toast.error('File type not supported or exceeds 10MB')
      return
    }
    setFile(accepted[0])
    if (!title) setTitle(accepted[0].name.replace(/\.[^/.]+$/, ''))
  }, [title])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: ACCEPTED_TYPES,
    maxSize: 10 * 1024 * 1024,
    multiple: false,
  })

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) { toast.error('Please select a file'); return }
    if (!title.trim()) { toast.error('Title is required'); return }

    setUploading(true)
    setProgress(0)

    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', title.trim())
    if (description.trim()) formData.append('description', description.trim())

    try {
      // Simulate progress
      const interval = setInterval(() => setProgress(p => Math.min(p + 12, 85)), 200)
      await documentApi.upload(formData)
      clearInterval(interval)
      setProgress(100)
      toast.success('Document uploaded successfully')
      setTimeout(() => navigate('/documents'), 800)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Upload failed')
      setProgress(0)
    }
    setUploading(false)
  }

  const fileSize = file ? (file.size / 1024 < 1024
    ? `${(file.size / 1024).toFixed(1)} KB`
    : `${(file.size / (1024 * 1024)).toFixed(2)} MB`) : null

  return (
    <div className="animate-fadeUp max-w-2xl mx-auto">
      <div className="mb-8">
        <Link to="/documents" className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-6">
          <ArrowLeft size={12} /> Back to Documents
        </Link>
        <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— New Document</div>
        <h1 className="page-header">Upload Document</h1>
        <p className="font-body text-muted mt-2 text-sm">
          Files are SHA-256 hashed on upload. Duplicates are automatically detected.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Drop zone */}
        <div>
          <label className="label">Document File *</label>
          <div
            {...getRootProps()}
            className={`border-3 border-dashed cursor-pointer transition-all duration-200 p-10 text-center
              ${isDragActive
                ? 'border-accent bg-accent/5 shadow-brutal-accent translate-x-[-3px] translate-y-[-3px]'
                : file
                  ? 'border-success bg-success/5'
                  : 'border-ink hover:border-accent hover:bg-surface-1'
              }`}
          >
            <input {...getInputProps()} />

            {file ? (
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-ink border-3 border-ink flex items-center justify-center flex-shrink-0">
                    <FileText size={18} className="text-paper" />
                  </div>
                  <div className="text-left">
                    <div className="font-mono text-sm font-medium text-ink">{file.name}</div>
                    <div className="font-mono text-xs text-muted mt-0.5">{fileSize} · {file.type || 'Unknown type'}</div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); setFile(null) }}
                  className="p-2 border-3 border-ink hover:bg-ink hover:text-paper transition-all"
                >
                  <X size={14} />
                </button>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-3">
                <div className={`w-16 h-16 border-3 border-ink flex items-center justify-center transition-all ${isDragActive ? 'bg-accent' : 'bg-surface-1'}`}>
                  <Upload size={24} className={isDragActive ? 'text-paper' : 'text-ink'} />
                </div>
                <div>
                  <div className="font-mono text-sm font-medium text-ink">
                    {isDragActive ? 'Drop it here' : 'Drag & drop or click to browse'}
                  </div>
                  <div className="font-mono text-xs text-muted mt-1">
                    PDF, PNG, JPG, DOC, DOCX · Max 10MB
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Title */}
        <div>
          <label className="label">Document Title *</label>
          <input
            type="text"
            className="input-field"
            placeholder="e.g. Bachelor's Degree Certificate 2024"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        {/* Description */}
        <div>
          <label className="label">Description <span className="text-muted normal-case">(optional)</span></label>
          <textarea
            className="input-field resize-none"
            rows={3}
            placeholder="Additional context about this document..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        {/* Info box */}
        <div className="border-3 border-ink bg-surface-1 p-4">
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">What happens next</div>
          <div className="space-y-1.5">
            {[
              '01 — Document is hashed (SHA-256) and stored securely',
              '02 — Submit for review to initiate the verification process',
              '03 — Institution verifier reviews and approves or rejects',
              '04 — On approval, a public verification link is generated',
            ].map(step => (
              <div key={step} className="font-mono text-xs text-ink">{step}</div>
            ))}
          </div>
        </div>

        {/* Progress bar */}
        {uploading && (
          <div className="border-3 border-ink">
            <div
              className="h-2 bg-accent transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}

        {/* Submit */}
        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={uploading || !file}
            className="btn-primary flex-1 flex items-center justify-center gap-3"
          >
            {uploading ? (
              <>
                <div className="w-4 h-4 border-2 border-paper border-t-transparent animate-spin" />
                Uploading... {progress}%
              </>
            ) : progress === 100 ? (
              <><CheckCircle size={14} /> Uploaded!</>
            ) : (
              <><Upload size={14} /> Upload Document</>
            )}
          </button>
          <Link to="/documents" className="btn-outline px-6 flex items-center">
            Cancel
          </Link>
        </div>
      </form>
    </div>
  )
}
