import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'
import { Shield, Eye, EyeOff } from 'lucide-react'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [showPass, setShowPass] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await login(form)
      navigate('/dashboard')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-paper grid-cross flex">
      <div className="hidden lg:flex flex-col justify-between w-1/2 bg-ink border-r-3 border-ink p-12">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 border-3 border-paper flex items-center justify-center">
            <Shield size={18} className="text-paper" />
          </div>
          <span className="font-display text-xl font-bold text-paper">DocuVerify</span>
        </div>
        <div>
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-6">Platform Features</div>
          <div className="space-y-4">
            {[
              ['SHA-256 Hashing', 'Every document cryptographically fingerprinted'],
              ['JWT Auth', '15-min access tokens with refresh rotation'],
              ['Role-Based Access', 'User → Verifier → Admin hierarchy'],
              ['Public QR Verification', 'Tamper-proof public document links'],
              ['Immutable Audit Logs', 'Every state change permanently recorded'],
            ].map(([title, desc]) => (
              <div key={title} className="border-l-3 border-accent pl-4">
                <div className="font-mono text-sm font-medium text-paper">{title}</div>
                <div className="font-mono text-xs text-muted">{desc}</div>
              </div>
            ))}
          </div>
        </div>
        <div>
          <div className="font-mono text-xs text-muted mb-3 border-t border-muted pt-4">Default credentials</div>
          <div className="font-mono text-xs text-accent">admin@docuverify.com</div>
          <div className="font-mono text-xs text-muted">Admin@123456</div>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          <div className="mb-10">
            <div className="font-mono text-xs text-muted tracking-widest uppercase mb-3">— Authenticate</div>
            <h1 className="font-display text-5xl font-bold text-ink leading-tight">Sign In</h1>
          </div>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="label">Email Address</label>
              <input type="email" className="input-field" placeholder="you@institution.edu"
                value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            </div>
            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input type={showPass ? 'text' : 'password'} className="input-field pr-12"
                  placeholder="••••••••" value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })} required />
                <button type="button" onClick={() => setShowPass(!showPass)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-muted hover:text-ink transition-colors">
                  {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>
            <button type="submit" disabled={loading}
              className="btn-primary w-full flex items-center justify-center gap-3">
              {loading ? <><div className="w-4 h-4 border-2 border-paper border-t-transparent animate-spin" />Authenticating...</> : 'Sign In →'}
            </button>
          </form>
          <div className="section-rule" />
          <p className="font-mono text-xs text-muted text-center">
            No account?{' '}
            <Link to="/register" className="text-ink underline underline-offset-4 hover:text-accent transition-colors">
              Register here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
