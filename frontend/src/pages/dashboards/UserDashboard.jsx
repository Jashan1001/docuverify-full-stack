import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { documentApi, statsApi } from '../../services/api'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { FileText, CheckSquare, Clock, XCircle, Upload, ArrowRight, Circle } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

export default function UserDashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      statsApi.user(),
      documentApi.getMyDocuments(0, 5),
    ]).then(([statsRes, docsRes]) => {
      setStats(statsRes.data.data)
      setDocs(docsRes.data.data.content || [])
    }).catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false))
  }, [])

  const handleSubmit = async (docId) => {
    try {
      await documentApi.submitForReview(docId)
      toast.success('Submitted for review')
      const res = await documentApi.getMyDocuments(0, 5)
      setDocs(res.data.data.content || [])
      const sRes = await statsApi.user()
      setStats(sRes.data.data)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit')
    }
  }

  if (loading) return <LoadingSpinner text="Loading dashboard..." />

  return (
    <div className="animate-fadeUp">
      <div className="flex items-start justify-between mb-10">
        <div>
          <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-2">— My Workspace</div>
          <h1 className="page-header">Hello, {user?.fullName?.split(' ')[0]}.</h1>
          <p className="font-body text-muted mt-2 text-sm">Upload, track, and share your verified documents.</p>
        </div>
        <Link to="/documents/upload" className="btn-accent flex items-center gap-2">
          <Upload size={14} /> Upload Document
        </Link>
      </div>

      {/* Real stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-12">
          {[
            { label: 'Total', value: stats.total, icon: <FileText size={18} />, color: 'bg-ink text-paper' },
            { label: 'Approved', value: stats.approved, icon: <CheckSquare size={18} />, color: 'bg-success text-paper' },
            { label: 'Under Review', value: stats.underReview, icon: <Clock size={18} />, color: 'bg-accent text-paper' },
            { label: 'Rejected', value: stats.rejected, icon: <XCircle size={18} />, color: 'bg-danger text-paper' },
            { label: 'Pending Submit', value: stats.uploaded, icon: <Circle size={18} />, color: 'bg-surface-2 text-ink' },
          ].map(({ label, value, icon, color }) => (
            <div key={label} className="border-3 border-ink shadow-brutal bg-paper hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200">
              <div className={`${color} p-3 flex items-center justify-between border-b-3 border-ink`}>
                {icon}
                <span className="font-display text-2xl font-bold">{value}</span>
              </div>
              <div className="px-3 py-2">
                <span className="font-mono text-xs font-bold tracking-widest uppercase text-ink/80">{label}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Quick action: if user has docs pending submission */}
      {stats?.uploaded > 0 && (
        <div className="border-3 border-accent bg-accent/5 p-4 mb-8 flex items-center justify-between">
          <div className="font-mono text-sm text-ink">
            <span className="font-bold">{stats.uploaded}</span> document{stats.uploaded > 1 ? 's' : ''} waiting to be submitted for review.
          </div>
          <Link to="/documents" className="btn-accent py-2 px-4 text-xs flex items-center gap-2">
            View & Submit <ArrowRight size={12} />
          </Link>
        </div>
      )}

      {/* Recent documents */}
      <div>
        <div className="flex items-center justify-between mb-5">
          <div>
            <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-1">— Recent</div>
            <h2 className="font-display text-2xl font-bold text-ink">My Documents</h2>
          </div>
          <Link to="/documents" className="btn-outline flex items-center gap-2 py-2 px-4 text-xs">
            View All <ArrowRight size={12} />
          </Link>
        </div>

        {docs.length === 0 ? (
          <div className="border-3 border-dashed border-muted p-12 text-center">
            <FileText size={32} className="text-muted mx-auto mb-4 opacity-40" />
            <p className="font-mono text-sm text-muted">No documents yet.</p>
            <Link to="/documents/upload" className="btn-primary inline-flex mt-4 items-center gap-2">
              <Upload size={14} /> Upload First Document
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {docs.map((doc) => (
              <div key={doc.id} className="flex items-center justify-between p-5 border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 border-3 border-ink flex items-center justify-center bg-surface-1 flex-shrink-0">
                    <FileText size={16} />
                  </div>
                  <div>
                    <div className="font-mono text-sm font-medium text-ink">{doc.title}</div>
                    <div className="font-mono text-xs text-muted mt-0.5">
                      {doc.createdAt ? format(new Date(doc.createdAt), 'dd MMM yyyy') : '—'}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={doc.status} />
                  {doc.status === 'UPLOADED' && (
                    <button onClick={() => handleSubmit(doc.id)}
                      className="btn-accent py-1.5 px-3 text-xs">
                      Submit →
                    </button>
                  )}
                  {doc.status === 'APPROVED' && (
                    <button onClick={() => { navigator.clipboard.writeText(`${window.location.origin}/verify/${doc.verificationToken}`); toast.success('Link copied!') }}
                      className="font-mono text-xs text-accent underline underline-offset-4">
                      Copy Link
                    </button>
                  )}
                  <Link to={`/documents/${doc.id}`} className="text-muted hover:text-ink transition-colors">
                    <ArrowRight size={14} />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
