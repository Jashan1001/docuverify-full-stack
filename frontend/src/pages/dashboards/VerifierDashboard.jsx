import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { documentApi, statsApi } from '../../services/api'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { CheckSquare, Clock, XCircle, AlertTriangle, ArrowRight, CheckCircle, TrendingUp } from 'lucide-react'
import { formatDistanceToNow, format } from 'date-fns'
import toast from 'react-hot-toast'

export default function VerifierDashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [queue, setQueue] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      statsApi.verifier(),
      documentApi.getPending(0, 5),
    ]).then(([sRes, qRes]) => {
      setStats(sRes.data.data)
      setQueue(qRes.data.data.content || [])
    }).catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner text="Loading review panel..." />

  const approvalRate = stats && (stats.totalApproved + stats.totalRejected) > 0
    ? Math.round((stats.totalApproved / (stats.totalApproved + stats.totalRejected)) * 100)
    : 0

  return (
    <div className="animate-fadeUp">
      <div className="flex items-start justify-between mb-10">
        <div>
          <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-2">— Verifier Panel</div>
          <h1 className="page-header">Review Dashboard</h1>
          <p className="font-body text-muted mt-2 text-sm">Manage the verification queue for your institution.</p>
        </div>
        {stats?.queueSize > 0 && (
          <Link to="/review" className="btn-accent flex items-center gap-2">
            <CheckSquare size={14} /> Review Queue ({stats.queueSize})
          </Link>
        )}
      </div>

      {/* Urgent warning */}
      {stats?.urgentCount > 0 && (
        <div className="border-3 border-danger bg-danger/5 p-4 mb-8 flex items-center gap-3 shadow-brutal">
          <AlertTriangle size={18} className="text-danger flex-shrink-0" />
          <div>
            <span className="font-mono text-sm font-bold text-danger">{stats.urgentCount} urgent</span>
            <span className="font-mono text-sm text-ink"> — documents waiting over 48 hours. Review immediately.</span>
          </div>
          <Link to="/review" className="btn-danger ml-auto py-2 px-4 text-xs flex items-center gap-2">
            Review Now <ArrowRight size={12} />
          </Link>
        </div>
      )}

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-12">
          <div className="border-3 border-ink shadow-brutal bg-paper md:col-span-1">
            <div className="bg-accent text-paper p-4 flex items-center justify-between border-b-3 border-ink">
              <Clock size={20} />
              <span className="font-display text-3xl font-bold">{stats.queueSize}</span>
            </div>
            <div className="px-4 py-3">
              <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase">In Queue</div>
              {stats.urgentCount > 0 && (
                <div className="font-mono text-xs text-danger mt-1">{stats.urgentCount} urgent</div>
              )}
            </div>
          </div>

          <div className="border-3 border-ink shadow-brutal bg-paper">
            <div className="bg-success text-paper p-4 flex items-center justify-between border-b-3 border-ink">
              <CheckCircle size={20} />
              <span className="font-display text-3xl font-bold">{stats.totalApproved}</span>
            </div>
            <div className="px-4 py-3">
              <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase">Total Approved</div>
              <div className="font-mono text-xs text-success mt-1">+{stats.approvedToday} today</div>
            </div>
          </div>

          <div className="border-3 border-ink shadow-brutal bg-paper">
            <div className="bg-danger text-paper p-4 flex items-center justify-between border-b-3 border-ink">
              <XCircle size={20} />
              <span className="font-display text-3xl font-bold">{stats.totalRejected}</span>
            </div>
            <div className="px-4 py-3">
              <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase">Total Rejected</div>
              <div className="font-mono text-xs text-danger mt-1">+{stats.rejectedToday} today</div>
            </div>
          </div>
        </div>
      )}

      {/* Approval rate bar */}
      {stats && (stats.totalApproved + stats.totalRejected) > 0 && (
        <div className="border-3 border-ink bg-paper shadow-brutal p-5 mb-12">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <TrendingUp size={16} className="text-muted" />
              <span className="font-mono text-xs tracking-widest uppercase text-muted">Your Approval Rate</span>
            </div>
            <span className="font-display text-2xl font-bold text-ink">{approvalRate}%</span>
          </div>
          <div className="border-3 border-ink h-5 bg-surface-1">
            <div className="h-full bg-success transition-all duration-700"
              style={{ width: `${approvalRate}%` }} />
          </div>
          <div className="flex justify-between mt-1">
            <span className="font-mono text-[10px] text-muted">0%</span>
            <span className="font-mono text-[10px] text-muted">100%</span>
          </div>
        </div>
      )}

      {/* Queue preview */}
      <div>
        <div className="flex items-center justify-between mb-5">
          <div>
            <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-1">— Needs Attention</div>
            <h2 className="font-display text-2xl font-bold text-ink">Review Queue</h2>
          </div>
          <Link to="/review" className="btn-outline flex items-center gap-2 py-2 px-4 text-xs">
            View All <ArrowRight size={12} />
          </Link>
        </div>

        {queue.length === 0 ? (
          <div className="border-3 border-dashed border-muted p-12 text-center">
            <CheckSquare size={32} className="text-success mx-auto mb-4 opacity-60" />
            <p className="font-mono text-sm text-muted">Queue is clear — all caught up!</p>
          </div>
        ) : (
          <div className="space-y-3">
            {queue.map((doc) => {
              const isUrgent = doc.updatedAt &&
                (Date.now() - new Date(doc.updatedAt).getTime()) > 48 * 60 * 60 * 1000
              return (
                <Link key={doc.id} to={`/review/${doc.id}`}
                  className="block border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200 group">
                  {isUrgent && (
                    <div className="bg-danger border-b-3 border-ink px-5 py-1">
                      <span className="font-mono text-[10px] text-paper tracking-widest uppercase">⚠ Over 48 hours — urgent</span>
                    </div>
                  )}
                  <div className="flex items-center justify-between p-4">
                    <div>
                      <div className="font-mono text-sm font-medium text-ink">{doc.title}</div>
                      <div className="font-mono text-xs text-muted mt-0.5">
                        {doc.uploadedBy} · {doc.updatedAt ? formatDistanceToNow(new Date(doc.updatedAt), { addSuffix: true }) : '—'}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <StatusBadge status={doc.status} />
                      <ArrowRight size={14} className="text-muted group-hover:text-accent transition-colors" />
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
