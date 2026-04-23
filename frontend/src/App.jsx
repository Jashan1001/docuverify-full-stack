import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/layout/Layout'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import DocumentsPage from './pages/DocumentsPage'
import DocumentDetailPage from './pages/DocumentDetailPage'
import UploadPage from './pages/UploadPage'
import ReviewQueuePage from './pages/ReviewQueuePage'
import ReviewDetailPage from './pages/ReviewDetailPage'
import PublicVerifyPage from './pages/PublicVerifyPage'
import AdminUsersPage from './pages/AdminUsersPage'

// Lazy inline placeholder pages for institution admin + admin institution list
import { lazy, Suspense } from 'react'
import LoadingSpinner from './components/ui/LoadingSpinner'

const AdminInstitutionsPage = lazy(() => import('./pages/AdminInstitutionsPage'))
const InstitutionMembersPage = lazy(() => import('./pages/InstitutionMembersPage'))

import LandingPage from './pages/LandingPage'

const Page = ({ children }) => (
  <ProtectedRoute>
    <Layout>
      <Suspense fallback={<LoadingSpinner />}>{children}</Suspense>
    </Layout>
  </ProtectedRoute>
)

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify/:token" element={<PublicVerifyPage />} />

        <Route path="/dashboard" element={<Page><DashboardPage /></Page>} />
        <Route path="/documents" element={<Page><DocumentsPage /></Page>} />
        <Route path="/documents/upload" element={<Page><UploadPage /></Page>} />
        <Route path="/documents/:id" element={<Page><DocumentDetailPage /></Page>} />

        <Route path="/review" element={
          <ProtectedRoute requireVerifier>
            <Layout><ReviewQueuePage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/review/:id" element={
          <ProtectedRoute requireVerifier>
            <Layout><ReviewDetailPage /></Layout>
          </ProtectedRoute>
        } />

        <Route path="/admin/users" element={
          <ProtectedRoute requireAdmin>
            <Layout><AdminUsersPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/admin/institutions" element={
          <ProtectedRoute requireAdmin>
            <Layout><Suspense fallback={<LoadingSpinner />}><AdminInstitutionsPage /></Suspense></Layout>
          </ProtectedRoute>
        } />

        <Route path="/institution/members" element={
          <ProtectedRoute requireInstAdmin>
            <Layout><Suspense fallback={<LoadingSpinner />}><InstitutionMembersPage /></Suspense></Layout>
          </ProtectedRoute>
        } />

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  )
}
