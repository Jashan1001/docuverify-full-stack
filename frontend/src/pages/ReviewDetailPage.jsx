import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { documentApi, verificationApi } from '../services/api'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { ArrowLeft, FileText, CheckCircle, XCircle, ExternalLink } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

export default function ReviewDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [doc, setDoc] = useState(null)
  const [loading, setLoading] = useState(true)
  const [action, setAction] = useState(null) // 'approve' | 'reject'
  const [remarks, setRemarks] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [stamped, setStamped] = useState(null) // 'APPROVED' | 'REJECTED'

  useEffect(() => {
    const fetch = async () => {
      try {
        const res = await documentApi.getById(id)
        setDoc(res.data.data)
      } catch {
        toast.error('Document not found')
        navigate('/review')
      }
      setLoading(false)
    }
    fetch()
  }, [id, navigate])

  const handleDecision = async () => {
    if (action === 'reject' && !rejectionReason.trim()) {
      toast.error('Rejection reason is required')
      return
    }
    setSubmitting(true)
    try {
      const payload = {
        documentId: id,
        remarks: remarks.trim() || undefined,
        rejectionReason: action === 'reject' ? rejectionReason.trim() : undefined,
      }
      if (action === 'approve') {
        const res = await verificationApi.approve(payload)
        setDoc(res.data.data)
        setStamped('APPROVED')
        toast.success('Document approved')
      } else {
        const res = await verificationApi.reject(payload)
        setDoc(res.data.data)
        setStamped('REJECTED')
        toast.success('Document rejected')
      }
      // Increased timeout to allow user to see the redirection link
      setTimeout(() => navigate('/review'), 6000)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed')
    }
    setSubmitting(false)
  }

  if (loading) return <LoadingSpinner text="Loading document..." />
  if (!doc) return null

  return (
    <div className="animate-fadeUp max-w-3xl mx-auto">
      <Link to="/review" className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-8">
        <ArrowLeft size={12} /> Back to Queue
      </Link>

      <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Review Decision</div>
      <h1 className="page-header mb-8">Review Document</h1>

      {/* Document card */}
      <div className="relative border-3 border-ink shadow-brutal-lg bg-paper mb-8">

        {/* Stamp overlay */}
        {stamped && (
          <div className="absolute inset-0 flex flex-col items-center justify-center z-20 bg-paper/90 backdrop-blur-sm p-8 text-center">
            <div className={`animate-stamp border-[6px] px-8 py-6 mb-8 rotate-[-8deg] shadow-brutal-lg
              ${stamped === 'APPROVED'
                ? 'border-success text-success'
                : 'border-danger text-danger'
              }`}>
              <div className="font-display text-5xl font-black tracking-widest">
                {stamped}
              </div>
              <div className="font-mono text-xs tracking-widest mt-1 opacity-70">
                {stamped === 'APPROVED' ? '✓ Verified & Sealed' : '✕ Document Rejected'}
              </div>
            </div>

            {stamped === 'APPROVED' && doc.verificationUrl && (
              <div className="animate-fadeUp delay-200 flex flex-col gap-4 w-full max-w-sm">
                <div className="font-mono text-xs text-ink font-bold uppercase tracking-widest">
                  Live Verification Proof Created
                </div>
                <div className="bg-surface-1 border-3 border-ink p-3 flex items-center gap-3">
                  <div className="flex-1 font-mono text-[10px] text-ink truncate select-all">
                    {doc.verificationUrl}
                  </div>
                  <button 
                    onClick={() => { navigator.clipboard.writeText(doc.verificationUrl); toast.success('Link copied!') }}
                    className="text-accent hover:text-ink transition-colors"
                  >
                    Copy
                  </button>
                </div>
                <a 
                  href={doc.verificationUrl} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="btn-primary py-3 px-6 text-xs flex items-center justify-center gap-2"
                >
                  <ExternalLink size={14} /> Open Public Verification Page
                </a>
              </div>
            )}
            
            <p className="mt-8 font-mono text-[10px] text-muted uppercase tracking-widest">
              Returning to queue in a few seconds...
            </p>
          </div>
        )}

        <div className="flex items-start justify-between p-6 border-b-3 border-ink">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 bg-ink border-3 border-ink flex items-center justify-center flex-shrink-0 shadow-brutal">
              <FileText size={20} className="text-paper" />
            </div>
            <div>
              <h2 className="font-display text-2xl font-bold text-ink">{doc.title}</h2>
              {doc.description && (
                <p className="font-body text-sm text-muted mt-1 max-w-lg">{doc.description}</p>
              )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 divide-x-3 divide-ink border-b-3 border-ink">
          {[
            ['Submitted By', doc.uploadedBy],
            ['Institution', doc.institutionName],
            ['File', doc.fileName],
            ['Size', `${(doc.fileSize / 1024).toFixed(1)} KB`],
            ['Type', doc.fileType],
            ['Date', doc.createdAt ? format(new Date(doc.createdAt), 'dd MMM yyyy, HH:mm') : '—'],
          ].map(([label, val]) => (
            <div key={label} className="px-5 py-4">
              <div className="font-mono text-xs font-black text-ink tracking-widest uppercase">{label}</div>
              <div className="font-mono text-sm text-ink mt-1 truncate">{val}</div>
            </div>
          ))}
        </div>

        <div className="p-5">
          <button
            onClick={() => {
              const loadingToast = toast.loading('Fetching document...');
              import('../services/api').then(({ fileApi }) => {
                fileApi.view(doc.fileUrl)
                  .then(() => toast.dismiss(loadingToast))
                  .catch(() => {
                    toast.dismiss(loadingToast);
                    toast.error('Failed to load document');
                  });
              });
            }}
            className="btn-outline inline-flex items-center gap-2 text-xs py-2 px-4"
          >
            <FileText size={12} /> View Original Document ↗
          </button>
        </div>
      </div>

      {/* Decision panel */}
      {!stamped && (
        <div className="border-3 border-ink shadow-brutal bg-paper p-6">
          <div className="font-mono text-xs text-muted tracking-widest uppercase mb-5">— Make Decision</div>

          {/* Action toggle */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            <button
              onClick={() => setAction('approve')}
              className={`flex items-center justify-center gap-3 py-5 border-3 font-mono text-sm font-medium transition-all duration-150
                ${action === 'approve'
                  ? 'bg-success text-paper border-ink shadow-brutal translate-x-[-2px] translate-y-[-2px]'
                  : 'bg-paper text-muted border-muted hover:border-ink hover:text-ink'
                }`}
            >
              <CheckCircle size={18} />
              Approve Document
            </button>
            <button
              onClick={() => setAction('reject')}
              className={`flex items-center justify-center gap-3 py-5 border-3 font-mono text-sm font-medium transition-all duration-150
                ${action === 'reject'
                  ? 'bg-danger text-paper border-ink shadow-brutal translate-x-[-2px] translate-y-[-2px]'
                  : 'bg-paper text-muted border-muted hover:border-ink hover:text-ink'
                }`}
            >
              <XCircle size={18} />
              Reject Document
            </button>
          </div>

          {action && (
            <div className="space-y-4 animate-fadeUp border-t-3 border-ink pt-5">
              {action === 'reject' && (
                <div>
                  <label className="label">
                    Rejection Reason <span className="text-danger">*</span>
                  </label>
                  <textarea
                    className="input-field resize-none"
                    rows={3}
                    placeholder="Explain clearly why this document is being rejected..."
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                  />
                </div>
              )}

              <div>
                <label className="label">Remarks <span className="text-muted normal-case">(optional)</span></label>
                <textarea
                  className="input-field resize-none"
                  rows={2}
                  placeholder="Internal notes for the audit log..."
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                />
              </div>

              <button
                onClick={handleDecision}
                disabled={submitting}
                className={`w-full flex items-center justify-center gap-3 font-mono text-sm font-medium py-4 border-3 border-ink shadow-brutal transition-all duration-150
                  hover:translate-x-[-2px] hover:translate-y-[-2px] hover:shadow-brutal-lg
                  active:translate-x-[2px] active:translate-y-[2px] active:shadow-none
                  disabled:opacity-50 disabled:transform-none
                  ${action === 'approve' ? 'bg-success text-paper' : 'bg-danger text-paper'}
                `}
              >
                {submitting ? (
                  <>
                    <div className="w-4 h-4 border-2 border-paper border-t-transparent animate-spin" />
                    Processing...
                  </>
                ) : action === 'approve' ? (
                  <><CheckCircle size={16} /> Confirm Approval</>
                ) : (
                  <><XCircle size={16} /> Confirm Rejection</>
                )}
              </button>
            </div>
          )}

          {!action && (
            <p className="font-mono text-xs text-muted text-center">
              Select a decision above to proceed.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
