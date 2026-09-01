import {
  createContext,
  useCallback,
  useContext,
  useState,
  type ReactNode,
} from 'react';

type ToastKind = 'info' | 'success' | 'error' | 'warn';
interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}
interface ToastContextValue {
  push: (message: string, kind?: ToastKind) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const push = useCallback((message: string, kind: ToastKind = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, kind, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4200);
  }, []);

  return (
    <ToastContext.Provider value={{ push }}>
      {children}
      <div className="toast-container" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.kind}`}>
            <span aria-hidden>
              {t.kind === 'success' ? '✓' : t.kind === 'error' ? '✕' : t.kind === 'warn' ? '⚠' : 'ℹ'}
            </span>
            <div className="toast-body">{t.message}</div>
            <button
              className="btn-icon"
              aria-label="Dismiss"
              onClick={() => setToasts((prev) => prev.filter((x) => x.id !== t.id))}
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
