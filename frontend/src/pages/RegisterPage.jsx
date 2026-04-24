import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'
import { Shield, Info, AlertTriangle } from 'lucide-react'

const PERSONAL_DOMAINS = new Set([
  'gmail.com', 'yahoo.com', 'hotmail.com', 'outlook.com',
  'icloud.com', 'protonmail.com', 'live.com', 'aol.com',
  'ymail.com', 'mail.com', 'zoho.com', 'rediffmail.com',
  'yandex.com', 'gmx.com', 'tutanota.com',
])

function getEmailDomain(email) {
  const idx = email.indexOf('@')
  return idx !== -1 ? email.slice(idx + 1).toLowerCase() : ''
}

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [loading, setLoading] = useState(false)

  const domain = getEmailDomain(form.email)
  const isPersonalEmail = domain.length > 0 && PERSONAL_DOMAINS.has(domain)
  const isInstitutionalEmail = domain.length > 0 && !PERSONAL_DOMAINS.has(domain)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.password.length < 8) {
      toast.error('Password must be at least 8 characters')
      return
    }
    setLoading(true)
    try {
      await register(form)
      toast.success('Account created. Welcome.')
      navigate('/dashboard')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-paper grid-cross flex items-center justify-center p-8">
      <div className="w-full max-w-lg">
        <Link to="/" className="flex items-center gap-3 mb-10 cursor-pointer hover:opacity-80 transition-opacity">
          <div className="w-10 h-10 bg-ink border-3 border-ink flex items-center justify-center shadow-brutal">
            <Shield size={18} className="text-paper" />
          </div>
          <div>
            <div className="font-display text-xl font-bold text-ink leading-none">DocuVerify</div>
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Trust Platform</div>
          </div>
        </Link>

        <div className="mb-8">
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-3">— Create Account</div>
          <h1 className="font-display text-5xl font-bold text-ink">Register</h1>
        </div>

        {/* Role info */}
        <div className="border-3 border-accent bg-accent/5 p-4 mb-6 flex gap-3">
          <Info size={16} className="text-accent flex-shrink-0 mt-0.5" />
          <div className="font-mono text-xs text-ink">
            Public registration creates a <strong>User</strong> account. Verifier and Admin roles
            are assigned by the platform administrator after account creation.
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="label">Full Name</label>
            <input
              type="text"
              className="input-field"
              placeholder="Your full legal name"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              required
            />
          </div>

          <div>
            <label className="label">Email Address</label>
            <input
              type="email"
              className="input-field"
              placeholder="you@institution.edu"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />

            {/* Institution auto-assignment hint */}
            {isInstitutionalEmail && (
              <div className="mt-2 flex items-start gap-2 border-3 border-success bg-success/5 px-3 py-2">
                <Info size={12} className="text-success flex-shrink-0 mt-0.5" />
                <span className="font-mono text-xs text-success">
                  Institutional email detected — you may be auto-assigned to your institution.
                </span>
              </div>
            )}

            {isPersonalEmail && (
              <div className="mt-2 flex items-start gap-2 border-3 border-accent bg-accent/5 px-3 py-2">
                <AlertTriangle size={12} className="text-accent flex-shrink-0 mt-0.5" />
                <span className="font-mono text-xs text-accent">
                  Personal email detected — you'll be placed in the Default Institution.
                  Use your institutional email (e.g. you@university.edu) to be auto-assigned.
                </span>
              </div>
            )}
          </div>

          <div>
            <label className="label">Password</label>
            <input
              type="password"
              className="input-field"
              placeholder="Minimum 8 characters"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
            {form.password.length > 0 && form.password.length < 8 && (
              <div className="mt-1 font-mono text-xs text-danger">
                {8 - form.password.length} more character{8 - form.password.length !== 1 ? 's' : ''} required
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={loading || form.password.length < 8}
            className="btn-primary w-full flex items-center justify-center gap-3 disabled:opacity-50"
          >
            {loading
              ? <><div className="w-4 h-4 border-2 border-paper border-t-transparent animate-spin" />Creating...</>
              : 'Create Account →'
            }
          </button>
        </form>

        <div className="section-rule" />
        <p className="font-mono text-xs text-muted text-center">
          Already registered?{' '}
          <Link to="/login" className="text-ink underline underline-offset-4 hover:text-accent transition-colors">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}