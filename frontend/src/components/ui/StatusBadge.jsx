const STATUS_STYLES = {
  UPLOADED: "bg-surface-2 text-ink border-ink",
  UNDER_REVIEW: "bg-accent text-paper border-ink",
  APPROVED: "bg-success text-paper border-ink",
  REJECTED: "bg-danger text-paper border-ink",
};

const STATUS_LABELS = {
  UPLOADED: "⬤ Uploaded",
  UNDER_REVIEW: "◈ Under Review",
  APPROVED: "✓ Approved",
  REJECTED: "✕ Rejected",
};

export default function StatusBadge({ status }) {
  if (status && !STATUS_STYLES[status]) {
    console.warn("Unknown document status:", status);
  }

  return (
    <span
      className={`status-badge ${STATUS_STYLES[status] || "bg-surface-2 border-ink"}`}
    >
      {STATUS_LABELS[status] || status}
    </span>
  );
}
