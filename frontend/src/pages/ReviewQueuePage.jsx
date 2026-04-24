import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { documentApi } from '../services/api'
import StatusBadge from '../components/ui/StatusBadge'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { ClipboardList, ChevronLeft, ChevronRight, Clock } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

export default function ReviewQueuePage() {
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)

  const fetchQueue = async (p = 0) => {
    setLoading(true)
    try {
      const res = await documentApi.getPending(p, 10)
      const data = res.data.data
      setDocs(data.content || [])
      setTotalPages(data.totalPages || 1)
      setTotalElements(data.totalElements || 0)
    } catch {
      toast.error('Failed to load review queue')
    }
    setLoading(false)
  }

  useEffect(() => { fetchQueue(page) }, [page])

  return (
    <div className="animate-fadeUp">
      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Pending Review</div>
          <h1 className="page-header">Review Queue</h1>
          <p className="font-body text-muted mt-2 text-sm">
            {totalElements > 0
              ? `${totalElements} document${totalElements !== 1 ? 's' : ''} awaiting review`
              : 'No documents pending review'}
          </p>
        </div>
        <div className="border-3 border-ink px-4 py-2 bg-surface-1 shadow-brutal">
          <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Queue</div>
          <div className="font-display text-2xl font-bold text-ink">{totalElements}</div>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading queue..." />
      ) : docs.length === 0 ? (
        <EmptyState
          icon={<ClipboardList size={48} />}
          title="Queue is clear"
          description="All documents have been reviewed. Check back later."
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
  const submittedAt = doc.updatedAt ? new Date(doc.updatedAt) : null
  const hoursWaiting = submittedAt
    ? Math.floor((Date.now() - submittedAt.getTime()) / (1000 * 60 * 60))
    : null
  const isUrgent = hoursWaiting !== null && hoursWaiting >= 48

  return (
    <Link
      to={`/review/${doc.id}`}
      className="block border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-3px] hover:translate-y-[-3px] hover:shadow-brutal-lg transition-all duration-200"
      style={{ animationDelay: `${index * 60}ms` }}
    >
      <div className="flex items-start justify-between p-5 border-b-3 border-ink">
        <div>
          <h3 className="font-display text-lg font-bold text-ink">{doc.title}</h3>
          {doc.description && (
            <p className="font-body text-sm text-muted mt-1 max-w-lg">{doc.description}</p>
          )}
        </div>
        <div className="flex items-center gap-2 flex-shrink-0 ml-4">
          {isUrgent && (
            <span className="border-2 border-danger text-danger font-mono text-[10px] tracking-widest uppercase px-2 py-0.5">
              Urgent
            </span>
          )}
          <StatusBadge status={doc.status} />
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 divide-x-3 divide-ink">
        {[
          ['Uploaded By', doc.uploadedBy],
          ['Institution', doc.institutionName],
          ['File', doc.fileName],
          ['Waiting',
            hoursWaiting !== null
              ? hoursWaiting < 1 ? 'Just now'
              : hoursWaiting < 24 ? `${hoursWaiting}h`
              : `${Math.floor(hoursWaiting / 24)}d ${hoursWaiting % 24}h`
              : '—'
          ],
        ].map(([label, val]) => (
          <div key={label} className="px-4 py-3">
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">{label}</div>
            <div className={`font-mono text-xs mt-0.5 truncate ${label === 'Waiting' && isUrgent ? 'text-danger font-bold' : 'text-ink'}`}>
              {val}
            </div>
          </div>
        ))}
      </div>

      <div className="px-5 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2 text-muted">
          <Clock size={12} />
          <span className="font-mono text-xs">
            Submitted {submittedAt ? format(submittedAt, 'dd MMM yyyy, HH:mm') : '—'}
          </span>
        </div>
        <span className="font-mono text-xs text-accent underline underline-offset-4">
          Review →
        </span>
      </div>
    </Link>
  )
}