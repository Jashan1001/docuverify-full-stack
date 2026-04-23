export default function LoadingSpinner({ text = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-24 gap-6">
      <div className="relative w-16 h-16">
        <div className="absolute inset-0 border-3 border-ink" />
        <div className="absolute inset-1 border-3 border-accent animate-spin" style={{ animationDuration: '1.5s' }} />
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-2 h-2 bg-ink" />
        </div>
      </div>
      <span className="font-mono text-xs tracking-widest uppercase text-muted">{text}</span>
    </div>
  )
}
