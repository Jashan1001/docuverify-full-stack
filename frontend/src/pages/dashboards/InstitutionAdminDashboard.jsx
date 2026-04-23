import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { documentApi, statsApi, adminApi } from '../../services/api'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { FileText, Users, CheckSquare, ArrowRight, Building2, Clock } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

export default function InstitutionAdminDashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [docs, setDocs] = useState([])
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      statsApi.institution(),
      documentApi.getPending(0, 5),
      adminApi.getInstitutionMembers(0, 5),
    ]).then(([sRes, dRes, mRes]) => {
      setStats(sRes.data.data)
      setDocs(dRes.data.data.content || [])
      setMembers(mRes.data.data.content || [])
    }).catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner text="Loading institution dashboard..." />

  return (
    <div className="animate-fadeUp">
      <div className="flex items-start justify-between mb-10">
        <div>
          <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-2">— Institution Admin</div>
          <h1 className="page-header">{stats?.institutionName || 'Institution'}</h1>
          <p className="font-body text-muted mt-2 text-sm">Manage your institution's documents and team.</p>
        </div>
        <Link to="/review" className="btn-outline flex items-center gap-2">
          <CheckSquare size={14} /> Review Queue
        </Link>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-12">
          {[
            { label: 'Total Documents', value: stats.totalDocuments, icon: <FileText size={18} />, color: 'bg-ink text-paper' },
            { label: 'Approved', value: stats.approvedDocuments, icon: <CheckSquare size={18} />, color: 'bg-success text-paper' },
            { label: 'Pending Review', value: stats.pendingDocuments, icon: <Clock size={18} />, color: 'bg-accent text-paper' },
            { label: 'Rejected', value: stats.rejectedDocuments, icon: <FileText size={18} />, color: 'bg-danger text-paper' },
            { label: 'Team Members', value: stats.totalMembers, icon: <Users size={18} />, color: 'bg-surface-2 text-ink' },
            { label: 'Verifiers', value: stats.verifierCount, icon: <CheckSquare size={18} />, color: 'bg-surface-2 text-ink' },
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

      <div className="grid md:grid-cols-2 gap-8">
        {/* Pending documents */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-1">— Queue</div>
              <h2 className="font-display text-xl font-bold text-ink">Pending Review</h2>
            </div>
            <Link to="/review" className="btn-outline py-2 px-4 text-xs flex items-center gap-1">
              View All <ArrowRight size={12} />
            </Link>
          </div>
          {docs.length === 0 ? (
            <div className="border-3 border-dashed border-muted p-8 text-center">
              <p className="font-mono text-xs text-muted">No pending documents</p>
            </div>
          ) : (
            <div className="space-y-2">
              {docs.map((doc) => (
                <Link key={doc.id} to={`/review/${doc.id}`}
                  className="flex items-center justify-between p-4 border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200 group">
                  <div>
                    <div className="font-mono text-sm font-medium text-ink">{doc.title}</div>
                    <div className="font-mono text-xs text-muted mt-0.5">{doc.uploadedBy}</div>
                  </div>
                  <ArrowRight size={14} className="text-muted group-hover:text-accent transition-colors" />
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Team members */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-1">— Team</div>
              <h2 className="font-display text-xl font-bold text-ink">Members</h2>
            </div>
            <Link to="/institution/members" className="btn-outline py-2 px-4 text-xs flex items-center gap-1">
              View All <ArrowRight size={12} />
            </Link>
          </div>
          {members.length === 0 ? (
            <div className="border-3 border-dashed border-muted p-8 text-center">
              <p className="font-mono text-xs text-muted">No team members yet</p>
              <p className="font-mono text-xs text-muted mt-1">Contact platform admin to assign roles.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {members.map((m) => (
                <div key={m.id} className="flex items-center justify-between p-4 border-3 border-ink bg-paper shadow-brutal">
                  <div>
                    <div className="font-mono text-sm font-medium text-ink">{m.fullName}</div>
                    <div className="font-mono text-xs text-muted mt-0.5">{m.email}</div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`status-badge text-[10px] ${m.enabled ? 'bg-success text-paper border-ink' : 'bg-muted text-paper border-ink'}`}>
                      {m.role.replace('ROLE_', '')}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
