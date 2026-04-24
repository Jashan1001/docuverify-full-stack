import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { publicApi, fileApi } from '../services/api'
import { Shield, CheckCircle, XCircle, Clock, AlertTriangle, FileText, ExternalLink } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

const STATUS_CONFIG = {
  APPROVED: {
    icon: <CheckCircle size={40} />,
    headline: 'Document Verified',
    subline: 'This document is authentic and has been officially approved.',
    stampText: 'VERIFIED',
    stampClass: 'border-success text-success',
    bgClass: 'bg-success/5',
    borderClass: 'border-success',
  },
  REJECTED: {
    icon: <XCircle size={40} />,
    headline: 'Document Rejected',
    subline: 'This document has been reviewed and rejected by the institution.',
    stampText: 'REJECTED',
    stampClass: 'border-danger text-danger',
    bgClass: 'bg-danger/5',
    borderClass: 'border-danger',
  },
  UNDER_REVIEW: {
    icon: <Clock size={40} />,
    headline: 'Under Review',
    subline: 'This document is currently being reviewed by the institution.',
    stampText: 'PENDING',
    stampClass: 'border-accent text-accent',
    bgClass: 'bg-accent/5',
    borderClass: 'border-accent',
  },
  UPLOADED: {
    icon: <AlertTriangle size={40} />,
    headline: 'Not Yet Submitted',
    subline: 'This document has not been submitted for verification yet.',
    stampText: 'UNVERIFIED',
    stampClass: 'border-muted text-muted',
    bgClass: 'bg-surface-1',
    borderClass: 'border-muted',
  },
}

export default function PublicVerifyPage() {
  const { token } = useParams()
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [stampVisible, setStampVisible] = useState(false)

  useEffect(() => {
    const verify = async () => {
      try {
        const res = await publicApi.verify(token)
        setResult(res.data.data)
        setTimeout(() => setStampVisible(true), 600)
      } catch (err) {
        setError(err.response?.data?.message || 'Verification link is invalid or expired.')
      }
      setLoading(false)
    }
    verify()
  }, [token])

  const handleViewFile = () => {
    if (!result?.fileUrl) return;
    const loadingToast = toast.loading('Opening document...');
    fileApi.view(result.fileUrl)
      .then(() => toast.dismiss(loadingToast))
      .catch(() => {
        toast.dismiss(loadingToast);
        toast.error('Failed to load document. It might have been uploaded before the storage migration.');
      });
  }

  const config = result ? STATUS_CONFIG[result.status] || STATUS_CONFIG.UPLOADED : null

  return (
    <div className="min-h-screen bg-paper grid-cross flex flex-col">
      {/* Top bar */}
      <div className="bg-ink border-b-3 border-ink">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-8 h-8 border-3 border-paper flex items-center justify-center">
              <Shield size={14} className="text-paper" />
            </div>
            <span className="font-display text-lg font-bold text-paper">DocuVerify</span>
          </Link>
          <span className="font-mono text-xs text-muted tracking-widest uppercase">
            Public Verification
          </span>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-2xl">

          {loading && (
            <div className="flex flex-col items-center gap-6 py-24">
              <div className="relative w-20 h-20">
                <div className="absolute inset-0 border-3 border-ink" />
                <div className="absolute inset-1 border-3 border-accent animate-spin" style={{ animationDuration: '1.5s' }} />
                <div className="absolute inset-0 flex items-center justify-center">
                  <Shield size={20} className="text-ink" />
                </div>
              </div>
              <div className="font-mono text-xs text-muted tracking-widest uppercase">
                Verifying document authenticity...
              </div>
            </div>
          )}

          {error && (
            <div className="border-3 border-danger shadow-brutal p-10 text-center bg-paper">
              <AlertTriangle size={40} className="text-danger mx-auto mb-4" />
              <h1 className="font-display text-3xl font-bold text-ink mb-2">Verification Failed</h1>
              <p className="font-body text-muted text-sm">{error}</p>
              <div className="section-rule" />
              <Link to="/" className="btn-outline inline-flex items-center gap-2 text-xs">
                <Shield size={12} /> Visit DocuVerify
              </Link>
            </div>
          )}

          {result && config && (
            <div className={`border-3 ${config.borderClass} shadow-brutal-lg bg-paper`}>

              {/* Status header */}
              <div className={`${config.bgClass} border-b-3 ${config.borderClass} p-8`}>
                <div className="flex items-start justify-between gap-6">
                  <div>
                    <div className={`${config.stampClass} mb-4`}>{config.icon}</div>
                    <h1 className="font-display text-4xl font-bold text-ink leading-tight">
                      {config.headline}
                    </h1>
                    <p className="font-body text-muted text-sm mt-2 max-w-sm">
                      {config.subline}
                    </p>
                  </div>

                  {/* The stamp */}
                  {stampVisible && (
                    <div className={`animate-stamp border-[5px] ${config.stampClass} px-5 py-4 text-center flex-shrink-0 rotate-[-6deg]`}>
                      <div className="font-display text-2xl font-black tracking-widest">
                        {config.stampText}
                      </div>
                      <div className="font-mono text-[9px] tracking-widest mt-1 opacity-60">
                        DOCUVERIFY
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Document metadata */}
              <div className="divide-y-3 divide-ink">
                <div className="grid grid-cols-2 divide-x-3 divide-ink">
                  <div className="px-6 py-4">
                    <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Document Title</div>
                    <div className="font-display text-lg font-bold text-ink mt-1">{result.title}</div>
                  </div>
                  <div className="px-6 py-4">
                    <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Issued By</div>
                    <div className="font-mono text-sm text-ink mt-1">{result.institutionName}</div>
                  </div>
                </div>

                <div className="grid grid-cols-2 divide-x-3 divide-ink">
                  <div className="px-6 py-4">
                    <div className="font-mono text-[10px] text-muted tracking-widest uppercase">Document Holder</div>
                    <div className="font-mono text-sm text-ink mt-1">{result.uploadedBy}</div>
                  </div>
                  <div className="px-6 py-4">
                    <div className="font-mono text-[10px] text-muted tracking-widest uppercase">
                      {result.status === 'APPROVED' ? 'Verified On' : 'Status'}
                    </div>
                    <div className="font-mono text-sm text-ink mt-1">
                      {result.status === 'APPROVED' && result.verifiedAt
                        ? format(new Date(result.verifiedAt), 'dd MMM yyyy, HH:mm')
                        : result.status.replace('_', ' ')}
                    </div>
                  </div>
                </div>
              </div>

              {/* View Document Action */}
              <div className="p-6 border-b-3 border-ink bg-surface-1">
                 <button 
                  onClick={handleViewFile}
                  className="w-full btn-primary flex items-center justify-center gap-3 py-4"
                >
                  <FileText size={18} />
                  View Verified Document
                  <ExternalLink size={14} />
                </button>
              </div>

              {/* Tamper notice */}
              <div className="px-6 py-4 flex items-center gap-3">
                <Shield size={14} className="text-muted flex-shrink-0" />
                <span className="font-mono text-xs text-muted">
                  This verification record is immutable and cryptographically sealed.
                  Verification ID: <span className="text-ink font-medium">{token.slice(0, 8).toUpperCase()}...{token.slice(-4).toUpperCase()}</span>
                </span>
              </div>

              {/* Footer actions */}
              <div className="px-6 py-4 border-t-3 border-ink flex items-center justify-between">
                <span className="font-mono text-xs text-muted">
                  Verified via DocuVerify Trust Platform
                </span>
                <a
                  href={window.location.href}
                  onClick={(e) => { e.preventDefault(); window.print() }}
                  className="btn-outline py-2 px-4 text-xs"
                >
                  Print / Save PDF
                </a>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
