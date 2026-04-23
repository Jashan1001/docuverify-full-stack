export default function EmptyState({ icon, title, description, action }) {
  return (
    <div className="border-3 border-dashed border-muted p-16 flex flex-col items-center justify-center text-center gap-4">
      {icon && <div className="text-muted opacity-40 mb-2">{icon}</div>}
      <h3 className="font-display text-2xl font-bold text-ink">{title}</h3>
      {description && <p className="font-body text-muted max-w-sm text-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
