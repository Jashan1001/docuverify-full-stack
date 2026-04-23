import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../services/api'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { ArrowLeft, Plus, Building2, ToggleLeft, ToggleRight, ChevronLeft, ChevronRight } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

export default function AdminInstitutionsPage() {
  const [institutions, setInstitutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', domain: '', contactEmail: '' })
  const [creating, setCreating] = useState(false)

  const fetchInstitutions = async (p = 0) => {
    setLoading(true)
    try {
      const res = await adminApi.getInstitutions(p, 10)
      setInstitutions(res.data.data.content || [])
      setTotalPages(res.data.data.totalPages || 1)
    } catch { toast.error('Failed to load institutions') }
    setLoading(false)
  }

  useEffect(() => { fetchInstitutions(page) }, [page])

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    try {
      await adminApi.createInstitution(form)
      toast.success(`"${form.name}" created`)
      setForm({ name: '', domain: '', contactEmail: '' })
      setShowForm(false)
      fetchInstitutions(0)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create')
    }
    setCreating(false)
  }

  const handleToggle = async (id, name) => {
    try {
      await adminApi.toggleInstitution(id)
      toast.success(`${name} updated`)
      fetchInstitutions(page)
    } catch { toast.error('Failed to update') }
  }

  return (
    <div className="animate-fadeUp">
      <Link to="/dashboard" className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-8">
        <ArrowLeft size={12} /> Back to Dashboard
      </Link>

      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Tenant Management</div>
          <h1 className="page-header">Institutions</h1>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary flex items-center gap-2">
          <Plus size={14} /> Create Institution
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="border-3 border-accent bg-accent/5 p-6 mb-8 shadow-brutal-accent animate-fadeUp">
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-4">— New Institution</div>
          <div className="grid md:grid-cols-3 gap-4 mb-4">
            <div>
              <label className="label">Name</label>
              <input className="input-field" placeholder="IIT Delhi" value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            </div>
            <div>
              <label className="label">Domain</label>
              <input className="input-field" placeholder="iitd.ac.in" value={form.domain}
                onChange={(e) => setForm({ ...form, domain: e.target.value })} required />
            </div>
            <div>
              <label className="label">Contact Email</label>
              <input type="email" className="input-field" placeholder="admin@iitd.ac.in" value={form.contactEmail}
                onChange={(e) => setForm({ ...form, contactEmail: e.target.value })} required />
            </div>
          </div>
          <div className="flex gap-3">
            <button type="submit" disabled={creating} className="btn-primary text-xs py-2 px-5 flex items-center gap-2">
              {creating ? 'Creating...' : <><Plus size={12} /> Create</>}
            </button>
            <button type="button" onClick={() => setShowForm(false)} className="btn-outline text-xs py-2 px-5">Cancel</button>
          </div>
        </form>
      )}

      {loading ? <LoadingSpinner /> : (
        <>
          <div className="grid gap-4 mb-8">
            {institutions.map((inst) => (
              <div key={inst.id} className="border-3 border-ink bg-paper shadow-brutal hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg transition-all duration-200">
                <div className="flex items-center justify-between p-5 border-b-3 border-ink">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 border-3 border-ink bg-surface-1 flex items-center justify-center flex-shrink-0 shadow-brutal">
                      <Building2 size={18} />
                    </div>
                    <div>
                      <div className="font-display text-lg font-bold text-ink">{inst.name}</div>
                      <div className="font-mono text-xs text-muted">{inst.domain} · {inst.contactEmail}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`status-badge border-2 ${inst.active ? 'bg-success text-paper border-ink' : 'text-muted border-muted'}`}>
                      {inst.active ? 'Active' : 'Inactive'}
                    </span>
                    <button onClick={() => handleToggle(inst.id, inst.name)}
                      className="text-muted hover:text-ink transition-colors p-1">
                      {inst.active ? <ToggleRight size={22} className="text-success" /> : <ToggleLeft size={22} />}
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-3 divide-x-3 divide-ink">
                  {[
                    ['Users', inst.userCount],
                    ['Documents', inst.documentCount],
                    ['Since', inst.createdAt ? format(new Date(inst.createdAt), 'MMM yyyy') : '—'],
                  ].map(([label, val]) => (
                    <div key={label} className="px-5 py-3">
                      <div className="font-mono text-[10px] text-muted tracking-widest uppercase">{label}</div>
                      <div className="font-mono text-sm text-ink font-medium mt-0.5">{val}</div>
                    </div>
                  ))}
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
              <span className="font-mono text-xs text-muted">Page {page + 1} of {totalPages}</span>
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
