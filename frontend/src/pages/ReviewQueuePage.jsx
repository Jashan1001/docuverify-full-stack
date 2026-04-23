import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { documentApi } from '../services/api'
import StatusBadge from '../components/ui/StatusBadge'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { CheckSquare, FileText, ArrowRight, ChevronLeft, ChevronRight, Clock } from 'lucide-react'
import { format, formatDistanceToNow } from 'date-fns'

export default function ReviewQueuePage() {
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)

  const fetchPending = async (p = 0) => {
    setLoading(true)
    try {
      const res = await documentApi.getPending(p, 10)
      const data = res.data.data
      setDocs(data.content || [])
      setTotalPages(data.totalPages || 1)
      setTotalElements(data.totalElements || 0)
    } catch {}
    setLoading(false)
  }

  useEffect(() => { fetchPending(page) }, [page])

  return (
    <div className="animate-fadeUp">
      <div className="flex items-start justify-between mb-10">
        <div>
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Verifier Panel</div>
          <h1 className="page-header">Review Queue</h1>
          <p className="font-body text-muted mt-2 text-sm">
            Documents awaiting your review and decision.
          </p>
        </div>
        {totalElements > 0 && (
          <div className="border-3 border-accent shadow-brutal-accent px-6 py-3 text-center">
            <div className="font-display text-3xl font-bold text-ink">{totalElements}</div>
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Pending</div>
          </div>
        )}
      </div>

      {/* Priority notice */}
      {totalElements > 5 && (
        <div className="border-3 border-accent bg-accent/10 p-4 mb-8 flex items-center gap-3">
          <Clock size={16} className="text-accent flex-shrink-0" />
          <span className="font-mono text-xs text-ink">
            {totalElements} documents in queue — oldest submissions take priority.
          </span>
        </div>
      )}

      {loading ? (
        <LoadingSpinner text="Loading review queue..." />
      ) : docs.length === 0 ? (
        <EmptyState
          icon={<CheckSquare size={48} />}
          title="Queue is clear"
          description="No documents are awaiting review. All caught up."
        />
      ) : (
        <>
          <div className="space-y-4">
            {docs.map((doc, i) => (
              <QueueCard key={doc.id} doc={doc} index={i} />
            ))}
          </div>

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

function QueueCard({ doc, index }) {
  const submittedAgo = doc.updatedAt
    ? formatDistanceToNow(new Date(doc.updatedAt), { addSuffix: true })
    : '—'

  const isUrgent = doc.createdAt &&
    (Date.now() - new Date(doc.createdAt).getTime()) > 48 * 60 * 60 * 1000

  return (
    <Link
      to={`/review/${doc.id}`}
      className="block border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-3px] hover:translate-y-[-3px] hover:shadow-brutal-lg transition-all duration-200 group"
      style={{ animationDelay: `${index * 60}ms` }}
    >
      {isUrgent && (
        <div className="bg-accent border-b-3 border-ink px-5 py-1.5">
          <span className="font-mono text-[10px] text-paper tracking-widest uppercase">
            ⚠ Submitted over 48 hours ago — review needed
          </span>
        </div>
      )}

      <div className="flex items-center justify-between p-5">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 border-3 border-ink flex items-center justify-center bg-surface-1 flex-shrink-0 shadow-brutal">
            <FileText size={18} />
          </div>
          <div>
            <h3 className="font-display text-lg font-bold text-ink leading-tight">{doc.title}</h3>
            <div className="font-mono text-xs text-muted mt-1">
              {doc.uploadedBy} · {doc.institutionName}
            </div>
            {doc.description && (
              <p className="font-body text-sm text-muted mt-1.5 max-w-md line-clamp-1">
                {doc.description}
              </p>
            )}
          </div>
        </div>

        <div className="flex flex-col items-end gap-3 flex-shrink-0 ml-4">
          <StatusBadge status={doc.status} />
          <div className="font-mono text-xs text-muted">{submittedAgo}</div>
          <ArrowRight size={16} className="text-muted group-hover:text-accent transition-colors" />
        </div>
      </div>

      <div className="grid grid-cols-3 divide-x-3 divide-ink border-t-3 border-ink">
        {[
          ['File', doc.fileName],
          ['Size', `${(doc.fileSize / 1024).toFixed(1)} KB`],
          ['Uploaded', doc.createdAt ? format(new Date(doc.createdAt), 'dd MMM yyyy') : '—'],
        ].map(([label, val]) => (
          <div key={label} className="px-4 py-2.5">
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">{label}</div>
            <div className="font-mono text-xs text-ink mt-0.5 truncate">{val}</div>
          </div>
        ))}
      </div>
    </Link>
  )
}
