import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../services/api'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { ArrowLeft, ChevronLeft, ChevronRight, ToggleLeft, ToggleRight, Shield } from 'lucide-react'
import toast from 'react-hot-toast'

const ROLES = ['', 'ROLE_USER', 'ROLE_VERIFIER', 'ROLE_INSTITUTION_ADMIN', 'ROLE_ADMIN']
const ASSIGNABLE_ROLES = ['ROLE_USER', 'ROLE_VERIFIER', 'ROLE_INSTITUTION_ADMIN']

export default function AdminUsersPage() {
  const [users, setUsers] = useState([])
  const [institutions, setInstitutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [roleFilter, setRoleFilter] = useState('')
  const [assignModal, setAssignModal] = useState(null) // user object
  const [assignForm, setAssignForm] = useState({ role: 'ROLE_USER', institutionId: '' })

  const fetchUsers = async (p = 0, role = '') => {
    setLoading(true)
    try {
      const res = await adminApi.getUsers(p, 10, role)
      setUsers(res.data.data.content || [])
      setTotalPages(res.data.data.totalPages || 1)
    } catch { toast.error('Failed to load users') }
    setLoading(false)
  }

  const fetchInstitutions = async () => {
    try {
      const res = await adminApi.getInstitutions(0, 50)
      setInstitutions(res.data.data.content || [])
    } catch {}
  }

  useEffect(() => { fetchUsers(page, roleFilter); fetchInstitutions() }, [page, roleFilter])

  const handleToggle = async (id, name) => {
    try {
      await adminApi.toggleUser(id)
      toast.success(`${name} updated`)
      fetchUsers(page, roleFilter)
    } catch (err) { toast.error(err.response?.data?.message || 'Failed') }
  }

  const handleAssignRole = async (e) => {
    e.preventDefault()
    try {
      await adminApi.assignRole({
        userId: assignModal.id,
        role: assignForm.role,
        institutionId: assignForm.institutionId || undefined,
      })
      toast.success(`Role assigned to ${assignModal.fullName}`)
      setAssignModal(null)
      fetchUsers(page, roleFilter)
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to assign role') }
  }

  const needsInstitution = ['ROLE_USER', 'ROLE_VERIFIER', 'ROLE_INSTITUTION_ADMIN'].includes(assignForm.role)

  return (
    <div className="animate-fadeUp">
      <Link to="/dashboard" className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-8">
        <ArrowLeft size={12} /> Back to Dashboard
      </Link>

      <div className="mb-8">
        <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Access Control</div>
        <h1 className="page-header">User Management</h1>
        <p className="font-body text-muted mt-2 text-sm">Assign roles, manage access, enable/disable accounts.</p>
      </div>

      {/* Role filter */}
      <div className="flex gap-2 flex-wrap mb-6 border-b-3 border-ink pb-4">
        {ROLES.map((r) => (
          <button key={r} onClick={() => { setRoleFilter(r); setPage(0) }}
            className={`font-mono text-xs tracking-widest uppercase px-4 py-2 border-3 transition-all duration-150 ${
              roleFilter === r ? 'bg-ink text-paper border-ink shadow-brutal' : 'border-ink text-muted hover:text-ink hover:bg-surface-1'
            }`}>
            {r ? r.replace('ROLE_', '') : 'ALL'}
          </button>
        ))}
      </div>

      {loading ? <LoadingSpinner text="Loading users..." /> : (
        <>
          <div className="border-3 border-ink shadow-brutal overflow-hidden mb-6">
            <div className="grid grid-cols-5 bg-ink text-paper border-b-3 border-ink">
              {['Name / Email', 'Role', 'Institution', 'Status', 'Actions'].map((h) => (
                <div key={h} className="px-4 py-3 font-mono text-[10px] tracking-widest uppercase">{h}</div>
              ))}
            </div>
            {users.length === 0 ? (
              <div className="p-8 text-center font-mono text-sm text-muted">No users found</div>
            ) : users.map((u, i) => (
              <div key={u.id}
                className={`grid grid-cols-5 items-center ${i < users.length - 1 ? 'border-b-3 border-ink' : ''} hover:bg-surface-1 transition-colors`}>
                <div className="px-4 py-3">
                  <div className="font-mono text-sm text-ink truncate">{u.fullName}</div>
                  <div className="font-mono text-xs text-muted truncate">{u.email}</div>
                </div>
                <div className="px-4 py-3">
                  <span className={`status-badge text-[10px] border-2 ${
                    u.role === 'ROLE_ADMIN' ? 'bg-ink text-paper border-ink' :
                    u.role === 'ROLE_INSTITUTION_ADMIN' ? 'bg-accent text-paper border-ink' :
                    u.role === 'ROLE_VERIFIER' ? 'bg-surface-2 text-ink border-ink' :
                    'border-ink text-muted'}`}>
                    {u.role.replace('ROLE_', '')}
                  </span>
                </div>
                <div className="px-4 py-3 font-mono text-xs text-muted truncate">{u.institutionName || '—'}</div>
                <div className="px-4 py-3">
                  <span className={`status-badge text-[10px] border-2 ${u.enabled ? 'text-success border-success' : 'text-danger border-danger'}`}>
                    {u.enabled ? 'Active' : 'Disabled'}
                  </span>
                </div>
                <div className="px-4 py-3 flex items-center gap-2">
                  {u.role !== 'ROLE_ADMIN' && (
                    <>
                      <button onClick={() => { setAssignModal(u); setAssignForm({ role: u.role, institutionId: u.institutionId || '' }) }}
                        className="font-mono text-[10px] text-accent underline underline-offset-2 hover:text-ink transition-colors">
                        Role
                      </button>
                      <span className="text-muted">·</span>
                      <button onClick={() => handleToggle(u.id, u.fullName)}
                        className="font-mono text-[10px] text-muted underline underline-offset-2 hover:text-ink transition-colors">
                        {u.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </>
                  )}
                  {u.role === 'ROLE_ADMIN' && (
                    <div className="flex items-center gap-1 text-muted">
                      <Shield size={12} />
                      <span className="font-mono text-[10px]">Protected</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t-3 border-ink pt-6">
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                className="btn-outline flex items-center gap-2 py-2 px-4 disabled:opacity-30">
                <ChevronLeft size={14} /> Previous
              </button>
              <span className="font-mono text-xs text-muted tracking-widest uppercase">Page {page + 1} of {totalPages}</span>
              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page === totalPages - 1}
                className="btn-outline flex items-center gap-2 py-2 px-4 disabled:opacity-30">
                Next <ChevronRight size={14} />
              </button>
            </div>
          )}
        </>
      )}

      {/* Assign role modal */}
      {assignModal && (
        <div className="fixed inset-0 bg-ink/60 flex items-center justify-center z-50 p-4">
          <div className="bg-paper border-3 border-ink shadow-brutal-lg w-full max-w-md">
            <div className="flex items-center justify-between p-5 border-b-3 border-ink">
              <div>
                <div className="font-mono text-xs text-muted tracking-widest uppercase">Assign Role</div>
                <div className="font-display text-xl font-bold text-ink mt-1">{assignModal.fullName}</div>
              </div>
              <button onClick={() => setAssignModal(null)} className="font-mono text-muted hover:text-ink text-xl">✕</button>
            </div>
            <form onSubmit={handleAssignRole} className="p-5 space-y-4">
              <div>
                <label className="label">New Role</label>
                <select className="input-field" value={assignForm.role}
                  onChange={(e) => setAssignForm({ ...assignForm, role: e.target.value })}>
                  {ASSIGNABLE_ROLES.map((r) => (
                    <option key={r} value={r}>{r.replace('ROLE_', '')}</option>
                  ))}
                </select>
              </div>
              {needsInstitution && (
                <div>
                  <label className="label">Institution *</label>
                  <select className="input-field" value={assignForm.institutionId}
                    onChange={(e) => setAssignForm({ ...assignForm, institutionId: e.target.value })} required>
                    <option value="">Select institution...</option>
                    {institutions.map((i) => (
                      <option key={i.id} value={i.id}>{i.name}</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="flex gap-3 pt-2">
                <button type="submit" className="btn-primary flex-1 text-xs py-3">Assign Role →</button>
                <button type="button" onClick={() => setAssignModal(null)} className="btn-outline px-6 text-xs">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
