import { useAuth } from '../context/AuthContext'
import UserDashboard from './dashboards/UserDashboard'
import VerifierDashboard from './dashboards/VerifierDashboard'
import InstitutionAdminDashboard from './dashboards/InstitutionAdminDashboard'
import AdminDashboard from './dashboards/AdminDashboard'

export default function DashboardPage() {
  const { user } = useAuth()
  const role = user?.role

  if (role === 'ROLE_ADMIN') return <AdminDashboard />
  if (role === 'ROLE_INSTITUTION_ADMIN') return <InstitutionAdminDashboard />
  if (role === 'ROLE_VERIFIER') return <VerifierDashboard />
  return <UserDashboard />
}
