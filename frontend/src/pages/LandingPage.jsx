import { Link } from 'react-router-dom'
import { ShieldCheck, FileText, CheckCircle, ArrowRight, UploadCloud } from 'lucide-react'

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-paper flex flex-col font-body">
      
      {/* Navigation */}
      <nav className="flex items-center justify-between p-6 border-b-3 border-ink bg-surface-1 shadow-brutal sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-accent border-3 border-ink flex items-center justify-center rotate-[-4deg] shadow-brutal">
            <ShieldCheck className="text-ink" size={24} />
          </div>
          <span className="font-display font-bold text-2xl tracking-tight text-ink uppercase">DocuVerify</span>
        </div>
        <div className="flex items-center gap-4">
          <Link to="/login" className="font-mono text-sm font-medium hover:text-accent transition-colors">
            LOG IN
          </Link>
          <Link to="/register" className="btn-primary py-2 px-5">
            GET STARTED
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="flex-1">
        <section className="grid-cross border-b-3 border-ink py-20 md:py-32 px-6 relative overflow-hidden">
          <div className="max-w-4xl mx-auto text-center relative z-10 animate-fadeUp">
            <div 
              className="inline-block bg-accent px-4 py-1.5 border-3 border-ink mb-8 rotate-[-2deg]"
              style={{ boxShadow: '2px 2px 0px #0D0D0D' }}
            >
              <span className="font-mono text-xs font-bold uppercase tracking-widest text-ink">
                Tamper-Proof Platform
              </span>
            </div>
            <h1 className="page-header text-5xl md:text-7xl mb-6">
              Immutable Document <br/><span className="text-accent underline decoration-ink decoration-4 underline-offset-8">Trust</span>
            </h1>
            <p className="text-lg md:text-xl text-muted max-w-2xl mx-auto font-medium mb-10 leading-relaxed">
              Cryptographically secure document verification for institutions. 
              Issue, verify, and prove authenticity without friction.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link to="/register" className="btn-primary text-lg px-8 py-4 w-full sm:w-auto flex items-center justify-center gap-3">
                Start Verifying <ArrowRight size={20} />
              </Link>
              <a href="#how-it-works" className="btn-outline text-lg px-8 py-4 w-full sm:w-auto">
                How It Works
              </a>
            </div>
          </div>
          
          {/* Decorative Elements */}
          <div className="absolute top-20 left-10 hidden lg:block opacity-40 hover:opacity-100 transition-opacity">
            <div className="w-24 h-24 border-3 border-ink bg-surface-2 animate-pulse-border transform rotate-6"></div>
          </div>
          <div className="absolute bottom-10 right-10 hidden lg:block opacity-40 hover:opacity-100 transition-opacity">
            <div className="w-32 h-32 rounded-full border-3 border-ink bg-surface-1 transform -rotate-12"></div>
          </div>
        </section>

        {/* Features Section */}
        <section id="how-it-works" className="py-24 px-6 bg-surface-1">
          <div className="max-w-6xl mx-auto">
            <div className="mb-16 text-center">
              <h2 className="font-display text-4xl md:text-5xl font-bold text-ink mb-4">How It Works</h2>
              <p className="font-mono text-sm uppercase tracking-widest text-muted">Three simple steps to absolute trust</p>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {/* Feature 1 */}
              <div className="card group hover:-translate-y-2 hover:-translate-x-2">
                <div className="w-16 h-16 bg-surface-2 border-3 border-ink flex items-center justify-center mb-6 shadow-brutal group-hover:bg-accent transition-colors">
                  <UploadCloud size={28} className="text-ink" />
                </div>
                <h3 className="font-display font-bold text-2xl mb-3">1. Upload & Hash</h3>
                <p className="text-muted font-medium text-sm leading-relaxed">
                  Upload your documents. We automatically compute a cryptographic SHA-256 hash to ensure it can never be altered without detection.
                </p>
              </div>

              {/* Feature 2 */}
              <div className="card group hover:-translate-y-2 hover:-translate-x-2">
                <div className="w-16 h-16 bg-surface-2 border-3 border-ink flex items-center justify-center mb-6 shadow-brutal group-hover:bg-success transition-colors">
                  <CheckCircle size={28} className="text-ink" />
                </div>
                <h3 className="font-display font-bold text-2xl mb-3">2. Verify & Sign</h3>
                <p className="text-muted font-medium text-sm leading-relaxed">
                  Authorized institutional verifiers review the document and cryptographically sign off on its authenticity, creating an immutable audit log.
                </p>
              </div>

              {/* Feature 3 */}
              <div className="card group hover:-translate-y-2 hover:-translate-x-2">
                <div className="w-16 h-16 bg-surface-2 border-3 border-ink flex items-center justify-center mb-6 shadow-brutal group-hover:bg-accent transition-colors">
                  <FileText size={28} className="text-ink" />
                </div>
                <h3 className="font-display font-bold text-2xl mb-3">3. Prove Anywhere</h3>
                <p className="text-muted font-medium text-sm leading-relaxed">
                  Share the unique public verification token. Anyone can verify the document's authenticity instantly, without needing an account.
                </p>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t-3 border-ink p-8 bg-paper text-center">
        <div className="font-mono text-sm font-bold uppercase tracking-widest text-ink mb-2">
          DocuVerify Platform
        </div>
        <div className="font-mono text-xs text-muted">
          &copy; {new Date().getFullYear()} DocuVerify. All rights reserved.
        </div>
      </footer>

    </div>
  )
}
