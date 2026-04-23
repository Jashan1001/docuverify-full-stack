import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { LogOut, FileText, CheckSquare, LayoutDashboard, Shield, Users, Building2 } from 'lucide-react'
import toast from 'react-hot-toast'

export default function Navbar() {
  const { user, logout, isVerifier, isAdmin } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const role = user?.role
  const isInstAdmin = role === 'ROLE_INSTITUTION_ADMIN'

  const handleLogout = async () => {
    await logout()
    toast.success('Signed out.')
    navigate('/login')
  }

  const isActive = (path) => location.pathname.startsWith(path) && path !== '/dashboard'
    ? true : location.pathname === path

  return (
    <header className="border-b-3 border-ink bg-paper sticky top-0 z-50">
      <div className="bg-ink text-paper font-mono text-xs py-1 px-6 flex items-center justify-between">
        <span className="tracking-widest uppercase opacity-60">DocuVerify Platform</span>
        {user && (
          <span className={`tracking-widest px-2 py-0.5 text-[10px] font-medium ${
            isAdmin ? 'bg-accent text-paper' :
            isInstAdmin ? 'bg-accent/60 text-paper' :
            isVerifier ? 'bg-surface-2 text-ink' : 'opacity-40 text-paper'
          }`}>
            {role?.replace('ROLE_', '')}
          </span>
        )}
      </div>
      <nav className="px-6 py-4 flex items-center justify-between max-w-7xl mx-auto">
        <Link to="/dashboard" className="flex items-center gap-3 group">
          <div className="w-10 h-10 bg-ink border-3 border-ink flex items-center justify-center shadow-brutal group-hover:translate-x-[-2px] group-hover:translate-y-[-2px] group-hover:shadow-brutal-lg transition-all duration-150">
            <Shield size={18} className="text-paper" />
          </div>
          <div>
            <div className="font-display text-xl font-bold text-ink leading-none">DocuVerify</div>
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Trust Platform</div>
          </div>
        </Link>

        {user && (
          <div className="hidden md:flex items-center gap-1">
            <NavLink to="/dashboard" active={location.pathname === '/dashboard'} icon={<LayoutDashboard size={14} />}>
              Dashboard
            </NavLink>

            {/* USER + all roles */}
            <NavLink to="/documents" active={isActive('/documents')} icon={<FileText size={14} />}>
              Documents
            </NavLink>

            {/* VERIFIER + INSTITUTION_ADMIN */}
            {(isVerifier || isInstAdmin) && (
              <NavLink to="/review" active={isActive('/review')} icon={<CheckSquare size={14} />}>
                Review Queue
              </NavLink>
            )}

            {/* INSTITUTION_ADMIN */}
            {isInstAdmin && (
              <NavLink to="/institution/members" active={isActive('/institution')} icon={<Users size={14} />}>
                My Team
              </NavLink>
            )}

            {/* ADMIN only */}
            {isAdmin && (
              <>
                <NavLink to="/admin/institutions" active={isActive('/admin/institutions')} icon={<Building2 size={14} />}>
                  Institutions
                </NavLink>
                <NavLink to="/admin/users" active={isActive('/admin/users')} icon={<Users size={14} />}>
                  Users
                </NavLink>
              </>
            )}
          </div>
        )}

        {user ? (
          <div className="flex items-center gap-4">
            <div className="hidden md:block text-right">
              <div className="font-mono text-xs font-medium text-ink">{user.fullName}</div>
              <div className="font-mono text-[10px] text-muted">{user.email}</div>
            </div>
            <button onClick={handleLogout} className="btn-outline flex items-center gap-2 py-2 px-4">
              <LogOut size={14} />
              <span className="hidden md:inline">Sign Out</span>
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <Link to="/login" className="btn-outline py-2 px-4">Login</Link>
            <Link to="/register" className="btn-primary py-2 px-4">Register</Link>
          </div>
        )}
      </nav>
    </header>
  )
}

function NavLink({ to, active, icon, children }) {
  return (
    <Link to={to} className={`flex items-center gap-2 font-mono text-xs tracking-widest uppercase px-4 py-2 border-3 transition-all duration-150 ${
      active ? 'bg-ink text-paper border-ink shadow-brutal' : 'border-transparent text-muted hover:border-ink hover:text-ink hover:bg-surface-1'
    }`}>
      {icon}{children}
    </Link>
  )
}
