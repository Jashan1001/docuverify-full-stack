import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { statsApi, adminApi } from '../../services/api'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { Building2, Users, FileText, CheckSquare, Plus, ArrowRight, ToggleLeft, ToggleRight, Shield } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [institutions, setInstitutions] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newInst, setNewInst] = useState({ name: '', domain: '', contactEmail: '' })
  const [creating, setCreating] = useState(false)

  const fetchAll = async () => {
    try {
      const [sRes, iRes, uRes] = await Promise.all([
        statsApi.admin(),
        adminApi.getInstitutions(0, 5),
        adminApi.getUsers(0, 8),
      ])
      setStats(sRes.data.data)
      setInstitutions(iRes.data.data.content || [])
      setUsers(uRes.data.data.content || [])
    } catch {
      toast.error('Failed to load admin dashboard')
    }
    setLoading(false)
  }

  useEffect(() => { fetchAll() }, [])

  const handleCreateInstitution = async (e) => {
    e.preventDefault()
    setCreating(true)
    try {
      await adminApi.createInstitution(newInst)
      toast.success(`Institution "${newInst.name}" created`)
      setNewInst({ name: '', domain: '', contactEmail: '' })
      setShowCreateForm(false)
      fetchAll()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create institution')
    }
    setCreating(false)
  }

  const handleToggleInstitution = async (id, name) => {
    try {
      await adminApi.toggleInstitution(id)
      toast.success(`${name} status updated`)
      fetchAll()
    } catch { toast.error('Failed to update') }
  }

  const handleToggleUser = async (id, name) => {
    try {
      await adminApi.toggleUser(id)
      toast.success(`${name} status updated`)
      fetchAll()
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to update') }
  }

  if (loading) return <LoadingSpinner text="Loading admin panel..." />

  return (
    <div className="animate-fadeUp">
      {/* Header */}
      <div className="flex items-start justify-between mb-10">
        <div>
          <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-2">— Platform Admin</div>
          <h1 className="page-header">Admin Panel</h1>
          <p className="font-body text-muted mt-2 text-sm">Full platform control — institutions, users, and oversight.</p>
        </div>
        <div className="flex items-center gap-2 border-3 border-ink px-4 py-2 bg-ink text-paper shadow-brutal">
          <Shield size={14} />
          <span className="font-mono text-xs tracking-widest uppercase">Superadmin</span>
        </div>
      </div>

      {/* Platform stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
          {[
            { label: 'Institutions', value: stats.totalInstitutions, sub: `${stats.activeInstitutions} active`, icon: <Building2 size={18} />, color: 'bg-ink text-paper' },
            { label: 'Total Users', value: stats.totalUsers, icon: <Users size={18} />, color: 'bg-surface-2 text-ink' },
            { label: 'All Documents', value: stats.totalDocuments, sub: `${stats.pendingDocuments} pending`, icon: <FileText size={18} />, color: 'bg-surface-2 text-ink' },
            { label: 'Verified Today', value: stats.verifiedToday, icon: <CheckSquare size={18} />, color: 'bg-success text-paper' },
          ].map(({ label, value, sub, icon, color }) => (
            <div key={label} className="border-3 border-ink shadow-brutal bg-paper hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200">
              <div className={`${color} p-4 flex items-center justify-between border-b-3 border-ink`}>
                {icon}
                <span className="font-display text-3xl font-bold">{value}</span>
              </div>
              <div className="px-4 py-3">
                <div className="font-mono text-xs font-bold tracking-widest uppercase text-ink/80">{label}</div>
                {sub && <div className="font-mono text-xs text-muted mt-0.5">{sub}</div>}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Institutions section */}
      <div className="mb-12">
        <div className="flex items-center justify-between mb-5">
          <div>
            <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-1">— Tenants</div>
            <h2 className="font-display text-2xl font-bold text-ink">Institutions</h2>
          </div>
          <div className="flex gap-2">
            <button onClick={() => setShowCreateForm(!showCreateForm)}
              className="btn-primary flex items-center gap-2 py-2 px-4 text-xs">
              <Plus size={14} /> Create Institution
            </button>
            <Link to="/admin/institutions" className="btn-outline flex items-center gap-2 py-2 px-4 text-xs">
              View All <ArrowRight size={12} />
            </Link>
          </div>
        </div>

        {/* Create institution form */}
        {showCreateForm && (
          <form onSubmit={handleCreateInstitution}
            className="border-3 border-accent bg-accent/5 p-6 mb-5 shadow-brutal-accent animate-fadeUp">
            <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-4">— New Institution</div>
            <div className="grid md:grid-cols-3 gap-4 mb-4">
              <div>
                <label className="label">Institution Name</label>
                <input className="input-field" placeholder="Chandigarh University"
                  value={newInst.name} onChange={(e) => setNewInst({ ...newInst, name: e.target.value })} required />
              </div>
              <div>
                <label className="label">Domain</label>
                <input className="input-field" placeholder="cu.ac.in"
                  value={newInst.domain} onChange={(e) => setNewInst({ ...newInst, domain: e.target.value })} required />
              </div>
              <div>
                <label className="label">Contact Email</label>
                <input type="email" className="input-field" placeholder="admin@cu.ac.in"
                  value={newInst.contactEmail} onChange={(e) => setNewInst({ ...newInst, contactEmail: e.target.value })} required />
              </div>
            </div>
            <div className="flex gap-3">
              <button type="submit" disabled={creating} className="btn-primary flex items-center gap-2 text-xs py-2 px-5">
                {creating ? 'Creating...' : <><Plus size={12} /> Create</>}
              </button>
              <button type="button" onClick={() => setShowCreateForm(false)} className="btn-outline text-xs py-2 px-5">Cancel</button>
            </div>
          </form>
        )}

        <div className="space-y-3">
          {institutions.map((inst) => (
            <div key={inst.id} className="border-3 border-ink bg-paper shadow-brutal">
              <div className="flex items-center justify-between p-4 border-b-3 border-ink">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 border-3 border-ink flex items-center justify-center bg-surface-1 flex-shrink-0">
                    <Building2 size={16} />
                  </div>
                  <div>
                    <div className="font-mono text-sm font-medium text-ink">{inst.name}</div>
                    <div className="font-mono text-xs text-muted">{inst.domain} · {inst.contactEmail}</div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`status-badge text-[10px] border-2 ${inst.active ? 'bg-success text-paper border-ink' : 'bg-muted text-paper border-ink'}`}>
                    {inst.active ? 'Active' : 'Inactive'}
                  </span>
                  <button onClick={() => handleToggleInstitution(inst.id, inst.name)}
                    className="text-muted hover:text-ink transition-colors">
                    {inst.active ? <ToggleRight size={20} className="text-success" /> : <ToggleLeft size={20} />}
                  </button>
                </div>
              </div>
              <div className="grid grid-cols-2 divide-x-3 divide-ink">
                <div className="px-4 py-2.5">
                  <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase">Users</div>
                  <div className="font-mono text-sm text-ink font-medium">{inst.userCount}</div>
                </div>
                <div className="px-4 py-2.5">
                  <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase">Documents</div>
                  <div className="font-mono text-sm text-ink font-medium">{inst.documentCount}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Users section */}
      <div>
        <div className="flex items-center justify-between mb-5">
          <div>
            <div className="font-mono text-xs font-bold text-ink/80 tracking-widest uppercase mb-1">— Access Control</div>
            <h2 className="font-display text-2xl font-bold text-ink">Users</h2>
          </div>
          <Link to="/admin/users" className="btn-outline flex items-center gap-2 py-2 px-4 text-xs">
            Manage All <ArrowRight size={12} />
          </Link>
        </div>

        <div className="border-3 border-ink overflow-hidden shadow-brutal">
          {/* Table header */}
          <div className="grid grid-cols-5 bg-ink text-paper border-b-3 border-ink">
            {['Name', 'Email', 'Role', 'Institution', 'Status'].map((h) => (
              <div key={h} className="px-4 py-3 font-mono text-xs font-black tracking-widest uppercase">{h}</div>
            ))}
          </div>
          {users.map((u, i) => (
            <div key={u.id}
              className={`grid grid-cols-5 items-center ${i < users.length - 1 ? 'border-b-3 border-ink' : ''} hover:bg-surface-1 transition-colors`}>
              <div className="px-4 py-3 font-mono text-sm text-ink truncate">{u.fullName}</div>
              <div className="px-4 py-3 font-mono text-xs text-muted truncate">{u.email}</div>
              <div className="px-4 py-3">
                <span className={`status-badge text-[10px] border-2 ${
                  u.role === 'ROLE_ADMIN' ? 'bg-ink text-paper border-ink' :
                  u.role === 'ROLE_INSTITUTION_ADMIN' ? 'bg-accent text-paper border-ink' :
                  u.role === 'ROLE_VERIFIER' ? 'bg-surface-2 text-ink border-ink' :
                  'border-ink text-muted'
                }`}>
                  {u.role.replace('ROLE_', '')}
                </span>
              </div>
              <div className="px-4 py-3 font-mono text-xs text-muted truncate">{u.institutionName || '—'}</div>
              <div className="px-4 py-3 flex items-center gap-2">
                <span className={`status-badge text-[10px] border-2 ${u.enabled ? 'text-success border-success' : 'text-danger border-danger'}`}>
                  {u.enabled ? 'Active' : 'Disabled'}
                </span>
                {u.role !== 'ROLE_ADMIN' && (
                  <button onClick={() => handleToggleUser(u.id, u.fullName)}
                    className="font-mono text-[10px] text-muted underline underline-offset-2 hover:text-ink transition-colors">
                    {u.enabled ? 'Disable' : 'Enable'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
