import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import LoadingSpinner from './ui/LoadingSpinner'

export default function ProtectedRoute({ children, requireVerifier, requireAdmin, requireInstAdmin }) {
  const { user, loading, isVerifier, isAdmin } = useAuth()
  const isInstAdmin = user?.role === 'ROLE_INSTITUTION_ADMIN' || isAdmin

  if (loading) return <LoadingSpinner text="Authenticating..." />
  if (!user) return <Navigate to="/login" replace />
  if (requireAdmin && !isAdmin) return <Navigate to="/dashboard" replace />
  if (requireInstAdmin && !isInstAdmin) return <Navigate to="/dashboard" replace />
  if (requireVerifier && !isVerifier && !isInstAdmin) return <Navigate to="/dashboard" replace />

  return children
}
