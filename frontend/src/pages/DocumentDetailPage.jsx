import { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { documentApi, verificationApi, fileApi } from "../services/api";
import api from "../services/api";
import StatusBadge from "../components/ui/StatusBadge";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import {
  ArrowLeft,
  FileText,
  Clock,
  Copy,
  QrCode,
  ShieldCheck,
  ShieldX,
  ExternalLink,
} from "lucide-react";
import { format } from "date-fns";
import toast from "react-hot-toast";

const ACTION_STYLES = {
  UPLOADED: "border-muted text-muted",
  SUBMITTED_FOR_REVIEW: "border-accent text-accent",
  APPROVED: "border-success text-success",
  REJECTED: "border-danger text-danger",
  PUBLIC_VERIFIED: "border-ink text-ink",
  VIEWED: "border-muted text-muted",
  DELETED: "border-danger text-danger",
};

export default function DocumentDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [doc, setDoc] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [qrUrl, setQrUrl] = useState(null);
  const [qrLoading, setQrLoading] = useState(false);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const docRes = await documentApi.getById(id);
        setDoc(docRes.data.data);
        try {
          const logRes = await verificationApi.getLogs(id);
          setLogs(logRes.data.data || []);
        } catch {}
      } catch {
        toast.error("Document not found");
        navigate("/documents");
      }
      setLoading(false);
    };
    fetchAll();
  }, [id, navigate]);

  // Fetch QR code as blob so the JWT is sent automatically via the api interceptor
  useEffect(() => {
    if (!doc || doc.status !== "APPROVED" || !doc.verificationToken) return;
    setQrLoading(true);
    api
      .get(`/documents/${doc.id}/qr`, { responseType: "blob" })
      .then((res) => setQrUrl(URL.createObjectURL(res.data)))
      .catch(() => toast.error("Could not load QR code"))
      .finally(() => setQrLoading(false));
  }, [doc]);

  // Clean up blob URL on unmount
  useEffect(() => {
    return () => {
      if (qrUrl) URL.revokeObjectURL(qrUrl);
    };
  }, [qrUrl]);

  const handleViewFile = () => {
    if (!doc?.fileUrl) return;
    const loadingToast = toast.loading('Opening document...');
    fileApi.view(doc.fileUrl)
      .then(() => toast.dismiss(loadingToast))
      .catch(() => {
        toast.dismiss(loadingToast);
        toast.error('Failed to load document. It might have been uploaded before the storage migration.');
      });
  }

  if (loading) return <LoadingSpinner text="Loading document..." />;
  if (!doc) return null;

  const verifyUrl = doc.verificationToken
    ? `${window.location.origin}/verify/${doc.verificationToken}`
    : null;

  const handleCopy = () => {
    if (!verifyUrl) return;
    navigator.clipboard.writeText(verifyUrl);
    toast.success("Verification link copied");
  };

  return (
    <div className="animate-fadeUp max-w-4xl mx-auto">
      <Link
        to="/documents"
        className="btn-outline inline-flex items-center gap-2 py-2 px-4 text-xs mb-8"
      >
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
              <div className="font-mono text-xs text-muted tracking-widest uppercase mb-1">
                Document
              </div>
              <h1 className="font-display text-3xl font-bold text-ink leading-tight">
                {doc.title}
              </h1>
              {doc.description && (
                <p className="font-body text-muted mt-2 text-sm max-w-lg">
                  {doc.description}
                </p>
              )}
            </div>
          </div>
          <div className="flex flex-col items-end gap-3">
             <StatusBadge status={doc.status} />
             <button 
                onClick={handleViewFile}
                className="btn-outline flex items-center gap-2 py-1.5 px-3 text-[10px] font-mono tracking-widest uppercase"
              >
                <ExternalLink size={10} /> View File
              </button>
          </div>
        </div>

        {/* Metadata grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 divide-x-3 divide-ink border-b-3 border-ink">
          {[
            ["File Name", doc.fileName],
            ["File Size", `${(doc.fileSize / 1024).toFixed(1)} KB`],
            ["Uploaded By", doc.uploadedBy],
            ["Institution", doc.institutionName],
          ].map(([label, val]) => (
            <div key={label} className="px-5 py-4">
              <div className="font-mono text-[10px] text-muted tracking-widest uppercase">
                {label}
              </div>
              <div className="font-mono text-sm text-ink mt-1 truncate">
                {val}
              </div>
            </div>
          ))}
        </div>

        <div className="grid grid-cols-2 divide-x-3 divide-ink">
          <div className="px-5 py-4">
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">
              Uploaded
            </div>
            <div className="font-mono text-sm text-ink mt-1">
              {doc.createdAt
                ? format(new Date(doc.createdAt), "dd MMM yyyy, HH:mm")
                : "—"}
            </div>
          </div>
          <div className="px-5 py-4">
            <div className="font-mono text-[10px] text-muted tracking-widest uppercase">
              Last Updated
            </div>
            <div className="font-mono text-sm text-ink mt-1">
              {doc.updatedAt
                ? format(new Date(doc.updatedAt), "dd MMM yyyy, HH:mm")
                : "—"}
            </div>
          </div>
        </div>
      </div>

      {/* ── Verification section — only shown when APPROVED ── */}
      {doc.status === "APPROVED" && verifyUrl && (
        <div className="border-3 border-success shadow-brutal bg-paper mb-8">
          {/* Section header */}
          <div className="flex items-center gap-3 px-6 py-4 border-b-3 border-success bg-success/5">
            <ShieldCheck size={18} className="text-success" />
            <span className="font-mono text-xs tracking-widest uppercase text-success font-bold">
              Document Verified — Share Proof
            </span>
          </div>

          <div className="flex flex-col md:flex-row divide-y-3 md:divide-y-0 md:divide-x-3 divide-ink">
            {/* QR Code */}
            <div className="flex flex-col items-center justify-center gap-3 p-6 flex-shrink-0">
              <div className="font-mono text-[10px] text-muted tracking-widest uppercase">
                <QrCode size={10} className="inline mr-1" />
                Scan to Verify
              </div>
              {qrLoading && (
                <div className="w-32 h-32 border-3 border-ink flex items-center justify-center bg-surface-1">
                  <div className="w-6 h-6 border-2 border-ink border-t-transparent animate-spin" />
                </div>
              )}
              {qrUrl && !qrLoading && (
                <img
                  src={qrUrl}
                  alt="Verification QR Code"
                  className="w-32 h-32 border-3 border-ink"
                />
              )}
              {qrUrl && (
                <a
                  href={qrUrl}
                  download={`${doc.title}-qr.png`}
                  className="btn-outline py-1.5 px-3 text-[10px] font-mono tracking-widest uppercase"
                >
                  Download QR
                </a>
              )}
            </div>

            {/* Verification link */}
            <div className="flex-1 p-6 flex flex-col justify-center gap-4">
              <div>
                <div className="font-mono text-[10px] text-muted tracking-widest uppercase mb-2">
                  Public Verification Link
                </div>
                <div className="flex items-center gap-2">
                  <div className="flex-1 border-3 border-ink bg-surface-1 px-3 py-2 font-mono text-xs text-ink truncate">
                    {verifyUrl}
                  </div>
                  <button
                    onClick={handleCopy}
                    className="btn-outline p-2 flex-shrink-0"
                    title="Copy link"
                  >
                    <Copy size={14} />
                  </button>
                </div>
              </div>

              <div className="border-3 border-ink bg-surface-1 p-4">
                <div className="font-mono text-[10px] text-muted tracking-widest uppercase mb-2">
                  How to share
                </div>
                <div className="space-y-1">
                  {[
                    "01 — Share the link or QR with employers, institutions, or auditors",
                    "02 — They can verify without creating an account",
                    "03 — Any file tampering will be detected automatically",
                  ].map((s) => (
                    <div key={s} className="font-mono text-xs text-ink">
                      {s}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── Rejection reason ── */}
      {doc.status === "REJECTED" && doc.rejectionReason && (
        <div className="border-3 border-danger shadow-brutal bg-paper mb-8">
          <div className="flex items-center gap-3 px-6 py-4 border-b-3 border-danger bg-danger/5">
            <ShieldX size={18} className="text-danger" />
            <span className="font-mono text-xs tracking-widest uppercase text-danger font-bold">
              Rejection Reason
            </span>
          </div>
          <div className="px-6 py-4">
            <p className="font-body text-sm text-ink">{doc.rejectionReason}</p>
          </div>
        </div>
      )}

      {/* ── Audit log ── */}
      {logs.length > 0 && (
        <div className="border-3 border-ink shadow-brutal bg-paper">
          <div className="px-6 py-4 border-b-3 border-ink flex items-center gap-3">
            <Clock size={14} className="text-muted" />
            <span className="font-mono text-xs tracking-widest uppercase text-muted">
              Audit Trail
            </span>
          </div>
          <div className="divide-y-3 divide-ink">
            {logs.map((log) => (
              <div key={log.id} className="flex items-start gap-4 px-6 py-4">
                <div
                  className={`border-2 px-2 py-0.5 font-mono text-[10px] tracking-widest uppercase flex-shrink-0 ${ACTION_STYLES[log.action] || "border-muted text-muted"}`}
                >
                  {log.action.replace("_", " ")}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-mono text-xs text-ink">
                    {log.remarks || "—"}
                  </div>
                  <div className="font-mono text-[10px] text-muted mt-0.5">
                    {log.performedBy || "Public"} ·{" "}
                    {log.ipAddress || "Unknown IP"}
                  </div>
                </div>
                <div className="font-mono text-[10px] text-muted flex-shrink-0">
                  {log.timestamp
                    ? format(new Date(log.timestamp), "dd MMM, HH:mm")
                    : "—"}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
