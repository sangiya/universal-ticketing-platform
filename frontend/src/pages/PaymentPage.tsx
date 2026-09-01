import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../context/ApiContext';
import {
  Alert,
  Currency,
  EmptyState,
  PageHeader,
  Skeleton,
} from '../components/UI';

interface Order {
  orderRef: string;
  productTitle: string;
  providerName: string;
  productType: string;
  quantity: number;
  unitPrice: number;
  currencyIso: string;
  baseAmount: number;
  taxAmount: number;
  serviceFee: number;
  discountAmount: number;
  totalAmount: number;
  status: string;
  holdExpiresAt?: string | null;
}

const CARD_TYPES = ['Visa', 'Mastercard', 'Amex'];

export default function PaymentPage() {
  const { orderRef } = useParams<{ orderRef: string }>();
  const { api } = useApi();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [cardNumber, setCardNumber] = useState('');
  const [cardType, setCardType] = useState('Visa');
  const [cardName, setCardName] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvv, setCvv] = useState('');
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<string | null>(null);
  const [paid, setPaid] = useState(false);
  const [nowTick, setNowTick] = useState(Date.now());

  useEffect(() => {
    const id = window.setInterval(() => setNowTick(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

  const loadOrder = useCallback(async () => {
    if (!orderRef) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<Order>(`/orders/${orderRef}`);
      setOrder(data);
      if (
        data.status === 'PAID' ||
        data.status === 'ISSUED' ||
        data.status === 'CONFIRMED'
      ) {
        setPaid(true);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load order');
    } finally {
      setLoading(false);
    }
  }, [api, orderRef]);

  useEffect(() => {
    void loadOrder();
  }, [loadOrder]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!order) return;
    setPaying(true);
    setPayError(null);
    try {
      await api.post<Order>(`/orders/${order.orderRef}/pay`);
      setPaid(true);
      setOrder((prev) => (prev ? { ...prev, status: 'PAID' } : prev));
    } catch (err) {
      setPayError(err instanceof Error ? err.message : 'Payment failed');
    } finally {
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <section className="page narrow">
        <PageHeader title="Checkout" subtitle="Loading your order…" />
        <div className="card">
          <Skeleton lines={6} />
        </div>
      </section>
    );
  }

  if (error || !order) {
    return (
      <section className="page narrow">
        <PageHeader title="Checkout" />
        <EmptyState
          icon="❌"
          title="Order not found"
          description={error ?? 'This order may have expired or been removed.'}
          action={
            <Link className="btn primary" to="/marketplace">
              Back to marketplace
            </Link>
          }
        />
      </section>
    );
  }

  if (paid) {
    return (
      <section className="page narrow">
        <div
          className="card spacious"
          style={{
            textAlign: 'center',
            borderTop: '4px solid var(--success)',
          }}
        >
          <div
            style={{
              width: 80,
              height: 80,
              margin: '0 auto var(--space-4)',
              borderRadius: '50%',
              background: 'var(--gradient-success)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '2.4rem',
              boxShadow: '0 12px 30px -8px rgba(16, 185, 129, 0.5)',
            }}
          >
            ✓
          </div>
          <h1 style={{ marginBottom: 'var(--space-2)' }}>Payment successful</h1>
          <p className="muted" style={{ maxWidth: 480, margin: '0 auto var(--space-5)' }}>
            Your order <strong>{order.orderRef}</strong> for{' '}
            <strong>{order.productTitle}</strong> (
            <Currency amount={order.totalAmount} currency={order.currencyIso} />
            ) is confirmed and paid. We've sent a receipt to your email.
          </p>
          <div className="row center" style={{ justifyContent: 'center' }}>
            <Link className="btn primary" to="/tickets">
              View my tickets
            </Link>
            <Link className="btn" to="/orders">
              My orders
            </Link>
            <Link className="btn ghost" to="/marketplace">
              Continue shopping
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const expired = order.holdExpiresAt
    ? new Date(order.holdExpiresAt).getTime() - nowTick <= 0
    : false;
  const remainMs = order.holdExpiresAt
    ? new Date(order.holdExpiresAt).getTime() - nowTick
    : 0;
  const m = Math.floor(remainMs / 60000);
  const s = Math.floor((remainMs % 60000) / 1000);

  return (
    <section className="page narrow">
      <PageHeader
        title="Checkout"
        subtitle="Review your order, apply a promo, and complete payment securely."
      />

      {expired && (
        <Alert kind="danger" title="Hold expired — inventory released">
          The reservation has expired. <Link to="/marketplace">Check availability again</Link>.
        </Alert>
      )}

      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-3)',
            marginBottom: 'var(--space-3)',
          }}
        >
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: 12,
              background: 'var(--gradient-soft)',
              color: 'var(--primary-700)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1.5rem',
            }}
          >
            🎟️
          </div>
          <div>
            <h3 style={{ margin: 0 }}>{order.productTitle}</h3>
            <p className="muted fs-sm" style={{ margin: 0 }}>
              {order.providerName} · {order.productType} · Qty {order.quantity}
            </p>
          </div>
        </div>

        <div className="divider" />

        <div className="stack">
          <div className="summary-row">
            <span className="muted">Base</span>
            <Currency amount={order.baseAmount} currency={order.currencyIso} />
          </div>
          <div className="summary-row">
            <span className="muted">Tax</span>
            <Currency amount={order.taxAmount} currency={order.currencyIso} />
          </div>
          <div className="summary-row">
            <span className="muted">Service fee</span>
            <Currency amount={order.serviceFee} currency={order.currencyIso} />
          </div>
          {order.discountAmount > 0 && (
            <div className="summary-row text-success">
              <span>Discount</span>
              <span>
                −<Currency amount={order.discountAmount} currency={order.currencyIso} />
              </span>
            </div>
          )}
          <div className="summary-row total">
            <span>Total</span>
            <Currency amount={order.totalAmount} currency={order.currencyIso} />
          </div>
        </div>

        <p
          className="muted fs-sm"
          style={{ marginTop: 'var(--space-3)', display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap' }}
        >
          <span>Order <code className="tag">{order.orderRef}</code></span>
          <span className="badge info">Pending payment</span>
          {order.holdExpiresAt && !expired && (
            <span className="badge warn">
              ⏱ Hold expires in {m}:{String(s).padStart(2, '0')}
            </span>
          )}
        </p>
      </div>

      <div className="card" style={{ borderTop: '4px solid var(--primary)' }}>
        <h3 style={{ marginBottom: 'var(--space-4)' }}>💳 Payment details</h3>
        <form className="form" onSubmit={(e) => void submit(e)} noValidate>
          <div className="field">
            <label htmlFor="cardType">Card type</label>
            <select
              id="cardType"
              value={cardType}
              onChange={(e) => setCardType(e.target.value)}
            >
              {CARD_TYPES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="cardName">Name on card</label>
            <input
              id="cardName"
              required
              value={cardName}
              onChange={(e) => setCardName(e.target.value)}
              placeholder="As shown on the card"
              autoComplete="cc-name"
            />
          </div>
          <div className="field">
            <label htmlFor="cardNumber">Card number</label>
            <input
              id="cardNumber"
              required
              value={cardNumber}
              onChange={(e) =>
                setCardNumber(
                  e.target.value
                    .replace(/\D/g, '')
                    .slice(0, 16)
                    .replace(/(.{4})/g, '$1 ')
                    .trim(),
                )
              }
              placeholder="4242 4242 4242 4242"
              inputMode="numeric"
              autoComplete="cc-number"
              maxLength={19}
            />
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="expiry">Expiry (MM/YY)</label>
              <input
                id="expiry"
                required
                value={expiry}
                onChange={(e) => {
                  const v = e.target.value.replace(/\D/g, '').slice(0, 4);
                  setExpiry(v.length >= 3 ? `${v.slice(0, 2)}/${v.slice(2)}` : v);
                }}
                placeholder="MM/YY"
                maxLength={5}
                autoComplete="cc-exp"
              />
            </div>
            <div className="field">
              <label htmlFor="cvv">CVV</label>
              <input
                id="cvv"
                required
                type="password"
                value={cvv}
                onChange={(e) => setCvv(e.target.value.replace(/\D/g, '').slice(0, 4))}
                placeholder="123"
                maxLength={4}
                autoComplete="cc-csc"
              />
            </div>
          </div>

          {payError && (
            <Alert kind="danger" title="Payment failed">
              {payError}
            </Alert>
          )}

          <button
            className="btn primary block lg"
            type="submit"
            disabled={paying || expired}
            aria-busy={paying}
          >
            {expired ? (
              'Hold expired'
            ) : paying ? (
              <>
                <span className="spinner" />
                Processing…
              </>
            ) : (
              `🔒 Pay ${order.currencyIso} ${Number(order.totalAmount).toFixed(2)}`
            )}
          </button>

          <p className="muted fs-xs text-center" style={{ margin: 0 }}>
            🔒 Demo checkout — payment is simulated. No real card is charged. In
            production, this is a PCI-aware flow with tokenization.
          </p>
        </form>
      </div>
    </section>
  );
}
