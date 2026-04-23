import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../services/api'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { ArrowLeft, Users, ChevronLeft, ChevronRight, Info } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

const ROLE_STYLES = {
  ROLE_VERIFIER: 'bg-surface-2 text-ink border-ink',
  ROLE_INSTITUTION_ADMIN: 'bg-accent text-paper border-ink',
  ROLE_USER: 'border-ink text-muted',
}

export default function InstitutionMembersPage() {
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)

  const fetchMembers = async (p = 0) => {
    setLoading(true)
    try {
      const res = await adminApi.getInstitutionMembers(p, 10)
      const data = res.data.data
      setMembers(data.content || [])
      setTotalPages(data.totalPages || 1)
      setTotalElements(data.totalElements || 0)
    } catch {
      toast.error('Failed to load team members')
    }
    setLoading(false)
  }

  useEffect(() => { fetchMembers(page) }, [page])

  const verifiers = members.filter(m => m.role === 'ROLE_VERIFIER')
  const admins = members.filter(m => m.role === 'ROLE_INSTITUTION_ADMIN')
  const users = members.filter(m => m.role === 'ROLE_USER')

  return (
    <div className="animate-fadeUp">
      <Link to="/dashboard" className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-8">
        <ArrowLeft size={12} /> Back to Dashboard
      </Link>

      <div className="flex items-start justify-between mb-10">
        <div>
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Institution Admin</div>
          <h1 className="page-header">My Team</h1>
          <p className="font-body text-muted mt-2 text-sm">All users belonging to your institution.</p>
        </div>
        <div className="border-3 border-ink shadow-brutal px-6 py-3 text-center">
          <div className="font-display text-3xl font-bold text-ink">{totalElements}</div>
          <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Total Members</div>
        </div>
      </div>

      {/* Role breakdown */}
      {!loading && (
        <div className="grid grid-cols-3 gap-4 mb-10">
          {[
            { label: 'Institution Admins', count: admins.length, color: 'bg-accent text-paper' },
            { label: 'Verifiers', count: verifiers.length, color: 'bg-surface-2 text-ink' },
            { label: 'Users', count: users.length, color: 'bg-paper text-ink' },
          ].map(({ label, count, color }) => (
            <div key={label} className="border-3 border-ink shadow-brutal bg-paper hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200">
              <div className={`${color} border-b-3 border-ink p-4 flex items-center justify-between`}>
                <Users size={18} />
                <span className="font-display text-3xl font-bold">{count}</span>
              </div>
              <div className="px-4 py-3">
                <span className="font-mono text-[10px] tracking-widest uppercase text-muted">{label}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Info notice */}
      <div className="border-3 border-ink bg-surface-1 p-4 mb-8 flex gap-3">
        <Info size={16} className="text-muted flex-shrink-0 mt-0.5" />
        <span className="font-mono text-xs text-muted">
          To assign or change roles for members, contact the Platform Admin.
          Only platform admins can modify roles and institution assignments.
        </span>
      </div>

      {loading ? <LoadingSpinner text="Loading team..." /> : (
        <>
          {members.length === 0 ? (
            <div className="border-3 border-dashed border-muted p-16 text-center">
              <Users size={40} className="text-muted mx-auto mb-4 opacity-40" />
              <p className="font-mono text-sm text-muted">No members in your institution yet.</p>
              <p className="font-mono text-xs text-muted mt-1">Ask the platform admin to assign users to your institution.</p>
            </div>
          ) : (
            <div className="border-3 border-ink shadow-brutal overflow-hidden">
              {/* Header */}
              <div className="grid grid-cols-4 bg-ink text-paper border-b-3 border-ink">
                {['Name', 'Email', 'Role', 'Joined'].map((h) => (
                  <div key={h} className="px-5 py-3 font-mono text-[10px] tracking-widest uppercase">{h}</div>
                ))}
              </div>

              {members.map((m, i) => (
                <div key={m.id}
                  className={`grid grid-cols-4 items-center ${i < members.length - 1 ? 'border-b-3 border-ink' : ''} hover:bg-surface-1 transition-colors`}>
                  <div className="px-5 py-4">
                    <div className="font-mono text-sm text-ink font-medium">{m.fullName}</div>
                    <div className={`w-2 h-2 rounded-full inline-block mr-1 mt-1 ${m.enabled ? 'bg-success' : 'bg-muted'}`} />
                    <span className="font-mono text-[10px] text-muted">{m.enabled ? 'Active' : 'Disabled'}</span>
                  </div>
                  <div className="px-5 py-4 font-mono text-xs text-muted truncate">{m.email}</div>
                  <div className="px-5 py-4">
                    <span className={`status-badge text-[10px] border-2 ${ROLE_STYLES[m.role] || 'border-ink text-muted'}`}>
                      {m.role.replace('ROLE_', '')}
                    </span>
                  </div>
                  <div className="px-5 py-4 font-mono text-xs text-muted">
                    {m.createdAt ? format(new Date(m.createdAt), 'dd MMM yyyy') : '—'}
                  </div>
                </div>
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-8 border-t-3 border-ink pt-6">
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                className="btn-outline flex items-center gap-2 py-2 px-4 disabled:opacity-30">
                <ChevronLeft size={14} /> Previous
              </button>
              <span className="font-mono text-xs text-muted tracking-widest uppercase">
                Page {page + 1} of {totalPages}
              </span>
              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page === totalPages - 1}
                className="btn-outline flex items-center gap-2 py-2 px-4 disabled:opacity-30">
                Next <ChevronRight size={14} />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
