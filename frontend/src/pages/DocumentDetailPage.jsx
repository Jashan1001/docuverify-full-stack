import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { documentApi, verificationApi } from '../services/api'
import StatusBadge from '../components/ui/StatusBadge'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { ArrowLeft, FileText, ExternalLink, Clock, Copy } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

const ACTION_STYLES = {
  UPLOADED: 'border-muted text-muted',
  SUBMITTED_FOR_REVIEW: 'border-accent text-accent',
  APPROVED: 'border-success text-success',
  REJECTED: 'border-danger text-danger',
  PUBLIC_VERIFIED: 'border-ink text-ink',
  VIEWED: 'border-muted text-muted',
  DELETED: 'border-danger text-danger',
}

export default function DocumentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [doc, setDoc] = useState(null)
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const docRes = await documentApi.getById(id)
        setDoc(docRes.data.data)
        try {
          const logRes = await verificationApi.getLogs(id)
          setLogs(logRes.data.data || [])
        } catch {}
      } catch {
        toast.error('Document not found')
        navigate('/documents')
      }
      setLoading(false)
    }
    fetchAll()
  }, [id, navigate])

  if (loading) return <LoadingSpinner text="Loading document..." />
  if (!doc) return null

  const verifyUrl = `${window.location.origin}/verify/${doc.verificationToken}`

  return (
    <div className="animate-fadeUp max-w-4xl mx-auto">
      <Link to="/documents" className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-8">
        <ArrowLeft size={12} /> Back to Documents
      </Link>

      {/* Header card */}
      <div className="border-3 border-ink shadow-brutal-lg bg-paper mb-8">
        <div className="flex items-start justify-between p-6 border-b-3 border-ink">
          <div className="flex items-start gap-5">
            <div className="w-16 h-16 bg-ink border-3 border-ink flex items-center justify-center flex-shrink-0 shadow-brutal">
              <FileText size={24} className="text-paper" />
            </div>
            <div>
              <div className="font-mono text-xs text-muted tracking-widest uppercase mb-1">Document</div>
              <h1 className="font-display text-3xl font-bold text-ink leading-tight">{doc.title}</h1>
              {doc.description && (
                <p className="font-body text-muted mt-2 text-sm max-w-lg">{doc.description}</p>
              )}
            </div>
          </div>
          <StatusBadge status={doc.status} />
        </div>

        {/* Metadata grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 divide-x-3 divide-ink border-b-3 border-ink">
          {[
            ['File Name', doc.fileName],
            ['File Size', `${(doc.fileSize / 1024).toFixed(1)} KB`],
            ['Uploaded By', doc.uploadedBy],
            ['Institution', doc.institutionName],
          ].map(([label, val]) => (
            <div key={label} className="px-5 py-4">
              <div className="font-mono text-xs font-black text-ink tracking-widest uppercase">{label}</div>
              <div className="font-mono text-sm text-ink mt-1 truncate">{val}</div>
            </div>
          ))}
        </div>
        <div className="grid grid-cols-2 divide-x-3 divide-ink border-b-3 border-ink">
          <div className="px-5 py-4">
            <div className="font-mono text-xs font-black text-ink tracking-widest uppercase">Uploaded</div>
            <div className="font-mono text-sm text-ink mt-1">
              {doc.createdAt ? format(new Date(doc.createdAt), 'dd MMM yyyy, HH:mm') : '—'}
            </div>
          </div>
          <div className="px-5 py-4">
            <div className="font-mono text-xs font-black text-ink tracking-widest uppercase">Last Updated</div>
            <div className="font-mono text-sm text-ink mt-1">
              {doc.updatedAt ? format(new Date(doc.updatedAt), 'dd MMM yyyy, HH:mm') : '—'}
            </div>
          </div>
        </div>

        {/* View Action */}
        <div className="p-5 bg-surface-1">
          <button
            onClick={() => {
              const loadingToast = toast.loading('Opening document...')
              import('../services/api').then(({ fileApi }) => {
                fileApi.view(doc.fileUrl)
                  .then(() => toast.dismiss(loadingToast))
                  .catch(() => {
                    toast.dismiss(loadingToast)
                    toast.error('Failed to load document. Please check your permissions.')
                  })
              })
            }}
            className="btn-primary w-full flex items-center justify-center gap-3"
          >
            <ExternalLink size={16} /> View Original Document ↗
          </button>
        </div>
      </div>

      {/* Rejection reason */}
      {doc.status === 'REJECTED' && doc.rejectionReason && (
        <div className="border-3 border-danger bg-danger/5 p-5 mb-8 shadow-brutal">
          <div className="font-mono text-xs text-danger tracking-widest uppercase mb-1">Rejection Reason</div>
          <p className="font-body text-ink text-sm">{doc.rejectionReason}</p>
        </div>
      )}

      {/* Public verification link */}
      {doc.status === 'APPROVED' && (
        <div className="border-3 border-success bg-success/5 p-5 mb-8 shadow-brutal">
          <div className="font-mono text-xs text-success tracking-widest uppercase mb-3">
            ✓ Verification Link — Share this to prove authenticity
          </div>
          <div className="flex items-center gap-3">
            <div className="flex-1 border-3 border-ink bg-paper px-4 py-2 font-mono text-xs text-muted truncate">
              {verifyUrl}
            </div>
            <button
              onClick={() => { navigator.clipboard.writeText(verifyUrl); toast.success('Copied!') }}
              className="btn-outline py-2 px-4 flex items-center gap-2 text-xs"
            >
              <Copy size={12} /> Copy
            </button>
            <Link
              to={`/verify/${doc.verificationToken}`}
              target="_blank"
              className="btn-primary py-2 px-4 flex items-center gap-2 text-xs"
            >
              <ExternalLink size={12} /> Open
            </Link>
          </div>
        </div>
      )}

      {/* Audit log timeline */}
      <div>
        <div className="font-mono text-xs text-muted tracking-widest uppercase mb-2">— Activity</div>
        <h2 className="font-display text-2xl font-bold text-ink mb-6">Audit Trail</h2>

        {logs.length === 0 ? (
          <div className="border-3 border-dashed border-muted p-8 text-center">
            <Clock size={24} className="text-muted mx-auto mb-3 opacity-40" />
            <p className="font-mono text-xs text-muted">No audit events recorded yet.</p>
          </div>
        ) : (
          <div className="relative">
            {/* Vertical line */}
            <div className="absolute left-6 top-0 bottom-0 w-[3px] bg-ink" />

            <div className="space-y-4 pl-16">
              {logs.map((log, i) => (
                <div
                  key={log.id || i}
                  className="relative border-3 border-ink bg-paper shadow-brutal p-4"
                  style={{ animationDelay: `${i * 50}ms` }}
                >
                  {/* Timeline dot */}
                  <div className="absolute -left-[46px] top-4 w-5 h-5 border-3 border-ink bg-paper flex items-center justify-center">
                    <div className="w-2 h-2 bg-ink" />
                  </div>

                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <span className={`status-badge border-2 text-[10px] ${ACTION_STYLES[log.action] || 'border-ink text-ink'}`}>
                        {log.action?.replace(/_/g, ' ')}
                      </span>
                      {log.performedBy && (
                        <div className="font-mono text-xs text-muted mt-2">
                          By: <span className="text-ink">{log.performedBy}</span>
                        </div>
                      )}
                      {log.ipAddress && (
                        <div className="font-mono text-xs text-muted">
                          IP: <span className="text-ink">{log.ipAddress}</span>
                        </div>
                      )}
                      {log.remarks && (
                        <div className="font-mono text-xs text-ink mt-1 border-l-3 border-accent pl-3 mt-2">
                          {log.remarks}
                        </div>
                      )}
                    </div>
                    <div className="text-right flex-shrink-0">
                      <div className="font-mono text-xs text-muted">
                        {log.timestamp ? format(new Date(log.timestamp), 'dd MMM yyyy') : '—'}
                      </div>
                      <div className="font-mono text-xs text-muted">
                        {log.timestamp ? format(new Date(log.timestamp), 'HH:mm:ss') : ''}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
