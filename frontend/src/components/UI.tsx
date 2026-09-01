import type { ReactNode } from 'react';

/** Page header with title, optional subtitle, and action slot. */
export function PageHeader({
  title,
  subtitle,
  actions,
  badge,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  badge?: ReactNode;
}) {
  return (
    <div className="page-header">
      <div className="page-title">
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          {title}
          {badge}
        </h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

/** Section title with optional subtitle, "view all" link, and badge. */
export function SectionTitle({
  title,
  subtitle,
  right,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  right?: ReactNode;
}) {
  return (
    <div className="section-title">
      <div>
        <h2 style={{ margin: 0 }}>{title}</h2>
        {subtitle && (
          <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>
            {subtitle}
          </p>
        )}
      </div>
      {right && <div className="muted">{right}</div>}
    </div>
  );
}

/** Skeleton placeholder while data is loading. */
export function Skeleton({
  lines = 3,
  height,
  width,
  className,
}: {
  lines?: number;
  height?: number | string;
  width?: number | string;
  className?: string;
}) {
  if (height || width) {
    return (
      <span
        className={`skeleton ${className ?? ''}`}
        style={{ display: 'inline-block', height, width }}
      />
    );
  }
  return (
    <div className={className}>
      {Array.from({ length: lines }).map((_, i) => (
        <div
          key={i}
          className="skeleton skeleton-line"
          style={{ width: `${85 - i * 8}%` }}
        />
      ))}
    </div>
  );
}

/** Empty state placeholder. */
export function EmptyState({
  icon = '📭',
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <div className="ico">{icon}</div>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}

/** Inline status dot. */
export function StatusDot({
  status,
  label,
}: {
  status: 'online' | 'busy' | 'offline';
  label?: string;
}) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
      <span className={`status-dot ${status}`} />
      {label && <span className="fs-sm muted">{label}</span>}
    </span>
  );
}

/** Reusable modal overlay. */
export function Modal({
  title,
  subtitle,
  description,
  children,
  footer,
  onClose,
  size,
  open = true,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  description?: ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
  onClose: () => void;
  size?: 'sm' | 'md' | 'lg';
  open?: boolean;
}) {
  if (!open) return null;
  const maxWidth = size === 'lg' ? 640 : size === 'sm' ? 380 : 480;
  return (
    <div
      className="modal-overlay"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="modal" style={{ maxWidth }}>
        <div className="modal-header">
          <div>
            <h3>{title}</h3>
            {subtitle && <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>{subtitle}</p>}
            {description && <p>{description}</p>}
          </div>
          <button className="btn-icon" aria-label="Close" onClick={onClose}>
            ✕
          </button>
        </div>
        {children && <div className="modal-body">{children}</div>}
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

/** KPI card. */
export function StatCard({
  label,
  value,
  delta,
  variant = 'default',
  icon,
}: {
  label: ReactNode;
  value: ReactNode;
  delta?: { value: string; dir: 'up' | 'down' };
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'violet';
  icon?: ReactNode;
}) {
  return (
    <div className={`stat ${variant === 'default' ? '' : variant}`}>
      <span className="label">
        {icon && <span>{icon}</span>}
        {label}
      </span>
      <span className="value">{value}</span>
      {delta && (
        <span className={`delta ${delta.dir}`}>
          {delta.dir === 'up' ? '↑' : '↓'} {delta.value}
        </span>
      )}
    </div>
  );
}

/** Lightweight alert banner. */
export function Alert({
  kind = 'info',
  title,
  children,
  onClose,
}: {
  kind?: 'info' | 'success' | 'warning' | 'danger';
  title?: ReactNode;
  children?: ReactNode;
  onClose?: () => void;
}) {
  const styles: Record<string, { bg: string; color: string; border: string; icon: string }> = {
    info: { bg: 'var(--info-50)', color: 'var(--info-700)', border: 'var(--info)', icon: 'ℹ' },
    success: { bg: 'var(--success-50)', color: 'var(--success-700)', border: 'var(--success)', icon: '✓' },
    warning: { bg: 'var(--warning-50)', color: 'var(--warning-700)', border: 'var(--warning)', icon: '⚠' },
    danger: { bg: 'var(--danger-50)', color: 'var(--danger-700)', border: 'var(--danger)', icon: '✕' },
  };
  const s = styles[kind];
  return (
    <div
      role="alert"
      style={{
        background: s.bg,
        color: s.color,
        borderLeft: `4px solid ${s.border}`,
        padding: 'var(--space-3) var(--space-4)',
        borderRadius: 'var(--radius-sm)',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '0.6rem',
      }}
    >
      <span style={{ fontWeight: 800 }}>{s.icon}</span>
      <div style={{ flex: 1 }}>
        {title && <div style={{ fontWeight: 700, marginBottom: 2 }}>{title}</div>}
        {children && <div style={{ fontSize: '0.88rem' }}>{children}</div>}
      </div>
      {onClose && (
        <button className="btn-icon" aria-label="Dismiss" onClick={onClose}>
          ✕
        </button>
      )}
    </div>
  );
}

/** Tag input. */
export function Currency({
  amount,
  currency,
  className,
}: {
  amount: number | string | null | undefined;
  currency?: string;
  className?: string;
}) {
  if (amount == null) return <span className={`muted ${className ?? ''}`}>—</span>;
  const formatted = Number(amount).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return (
    <span className={`currency ${className ?? ''}`}>
      {currency ? `${currency} ${formatted}` : formatted}
    </span>
  );
}
