import Navbar from './Navbar'

export default function Layout({ children }) {
  return (
    <div className="min-h-screen bg-paper grid-cross">
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 py-10">
        {children}
      </main>
      <footer className="border-t-3 border-ink mt-20">
        <div className="max-w-7xl mx-auto px-6 py-6 flex items-center justify-between">
          <span className="font-mono text-xs text-muted tracking-widest uppercase">
            DocuVerify © 2025 — All documents cryptographically sealed
          </span>
          <span className="font-mono text-xs text-muted">SHA-256 // JWT // RBAC</span>
        </div>
      </footer>
    </div>
  )
}
