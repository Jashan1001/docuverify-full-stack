import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { documentApi } from '../services/api'
import StatusBadge from '../components/ui/StatusBadge'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { FileText, Upload, ArrowRight, ChevronLeft, ChevronRight } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

const STATUS_FILTERS = ['ALL', 'UPLOADED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED']

export default function DocumentsPage() {
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [filter, setFilter] = useState('ALL')

  const fetchDocs = async (p = 0) => {
    setLoading(true)
    try {
      const res = await documentApi.getMyDocuments(p, 8, filter === 'ALL' ? '' : filter)
      const data = res.data.data
      setDocs(data.content || [])
      setTotalPages(data.totalPages || 1)
    } catch {
      toast.error('Failed to load documents')
    }
    setLoading(false)
  }

  useEffect(() => { fetchDocs(page) }, [page, filter])

  const handleFilterChange = (f) => {
    setFilter(f)
    setPage(0)
  }

  return (
    <div className="animate-fadeUp">
      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— My Files</div>
          <h1 className="page-header">Documents</h1>
        </div>
        <Link to="/documents/upload" className="btn-primary flex items-center gap-2">
          <Upload size={14} />
          Upload New
        </Link>
      </div>

      {/* Filters */}
      <div className="flex gap-2 flex-wrap mb-8 border-b-3 border-ink pb-4">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => handleFilterChange(f)}
            className={`font-mono text-xs tracking-widest uppercase px-4 py-2 border-3 transition-all duration-150 ${
              filter === f
                ? 'bg-ink text-paper border-ink shadow-brutal'
                : 'border-ink text-muted hover:text-ink hover:bg-surface-1'
            }`}
          >
            {f.replace('_', ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingSpinner text="Fetching documents..." />
      ) : docs.length === 0 ? (
        <EmptyState
          icon={<FileText size={48} />}
          title="No documents found"
          description="Upload your first document to get started with the verification process."
          action={<Link to="/documents/upload" className="btn-primary flex items-center gap-2"><Upload size={14} />Upload Document</Link>}
        />
      ) : (
        <>
          <div className="grid gap-4">
            {docs.map((doc, i) => (
              <DocumentCard key={doc.id} doc={doc} index={i} onRefresh={() => fetchDocs(page)} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-8 border-t-3 border-ink pt-6">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn-outline flex items-center gap-2 py-2 px-4 disabled:opacity-30"
              >
                <ChevronLeft size={14} /> Previous
              </button>
              <span className="font-mono text-xs text-muted tracking-widest uppercase">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page === totalPages - 1}
                className="btn-outline flex items-center gap-2 py-2 px-4 disabled:opacity-30"
              >
                Next <ChevronRight size={14} />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

function DocumentCard({ doc, index, onRefresh }) {
  const [submitting, setSubmitting] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    e.stopPropagation()
    setSubmitting(true)
    try {
      await documentApi.submitForReview(doc.id)
      toast.success('Submitted for review')
      onRefresh()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit')
    }
    setSubmitting(false)
  }

  const handleDelete = async (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (!confirm('Delete this document? This cannot be undone.')) return
    setDeleting(true)
    try {
      await documentApi.delete(doc.id)
      toast.success('Document deleted')
      onRefresh()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cannot delete')
    }
    setDeleting(false)
  }

  const verifyUrl = `${window.location.origin}/verify/${doc.verificationToken}`

  return (
    <div
      className="border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-3px] hover:translate-y-[-3px] hover:shadow-brutal-lg transition-all duration-200"
      style={{ animationDelay: `${index * 60}ms` }}
    >
      <div className="flex items-start justify-between p-5 border-b-3 border-ink">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 border-3 border-ink flex items-center justify-center bg-surface-1 flex-shrink-0 shadow-brutal">
            <FileText size={18} />
          </div>
          <div>
            <h3 className="font-display text-lg font-bold text-ink leading-tight">{doc.title}</h3>
            {doc.description && (
              <p className="font-body text-sm text-muted mt-1 max-w-lg">{doc.description}</p>
            )}
          </div>
        </div>
        <StatusBadge status={doc.status} />
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 divide-x-3 divide-ink border-b-3 border-ink">
        {[
          ['File', doc.fileName],
          ['Size', `${(doc.fileSize / 1024).toFixed(1)} KB`],
          ['Uploaded', doc.createdAt ? format(new Date(doc.createdAt), 'dd MMM yyyy') : '—'],
          ['Institution', doc.institutionName || '—'],
        ].map(([label, val]) => (
          <div key={label} className="px-4 py-3">
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">{label}</div>
            <div className="font-mono text-xs text-ink mt-0.5 truncate">{val}</div>
          </div>
        ))}
      </div>

      {doc.rejectionReason && (
        <div className="px-5 py-3 bg-danger border-b-3 border-ink">
          <span className="font-mono text-xs text-paper">
            Rejection reason: {doc.rejectionReason}
          </span>
        </div>
      )}

      <div className="flex items-center justify-between px-5 py-3">
        <div className="flex gap-2 flex-wrap">
          {doc.status === 'UPLOADED' && (
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="btn-accent py-2 px-4 text-xs flex items-center gap-1"
            >
              {submitting ? 'Submitting...' : 'Submit for Review →'}
            </button>
          )}
          {doc.status !== 'APPROVED' && (
            <button
              onClick={handleDelete}
              disabled={deleting}
              className="btn-danger py-2 px-4 text-xs"
            >
              {deleting ? 'Deleting...' : 'Delete'}
            </button>
          )}
          <Link
            to={`/documents/${doc.id}`}
            className="btn-outline py-2 px-4 text-xs flex items-center gap-1"
          >
            View Details <ArrowRight size={12} />
          </Link>
        </div>

        {doc.status === 'APPROVED' && (
          <button
            onClick={() => { navigator.clipboard.writeText(verifyUrl); toast.success('Verification link copied!') }}
            className="font-mono text-xs text-accent underline underline-offset-4 hover:text-ink transition-colors"
          >
            Copy Verify Link
          </button>
        )}
      </div>
    </div>
  )
}
