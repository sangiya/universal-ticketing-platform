import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../context/ApiContext';

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

  const loadOrder = useCallback(async () => {
    if (!orderRef) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<Order>(`/orders/${orderRef}`);
      setOrder(data);
      if (data.status === 'CONFIRMED') setPaid(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load order');
    } finally {
      setLoading(false);
    }
  }, [api, orderRef]);

  useEffect(() => {
    void loadOrder();
  }, [loadOrder]);

  const fmt = (n: number | null | undefined) => n == null ? '—' : Number(n).toFixed(2);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!order) return;
    setPaying(true);
    setPayError(null);
    try {
      await api.post<Order>(`/orders/${order.orderRef}/pay`);
      setPaid(true);
      setOrder((prev) => (prev ? { ...prev, status: 'CONFIRMED' } : prev));
    } catch (err) {
      setPayError(err instanceof Error ? err.message : 'Payment failed');
    } finally {
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <section className="page narrow">
        <p className="muted">Loading checkout…</p>
      </section>
    );
  }

  if (error || !order) {
    return (
      <section className="page narrow">
        <h1>Checkout</h1>
        <p className="error">{error ?? 'Order not found.'}</p>
        <Link className="btn" to="/marketplace">Back to marketplace</Link>
      </section>
    );
  }

  if (paid) {
    return (
      <section className="page narrow">
        <div className="card" style={{ textAlign: 'center', padding: '2rem' }}>
          <div style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>✔</div>
          <h1>Payment successful</h1>
          <p className="muted">
            Your order <strong>{order.orderRef}</strong> for{' '}
            <strong>{order.productTitle}</strong>{' '}
            ({order.currencyIso} {fmt(order.totalAmount)}) is confirmed and paid.
          </p>
          <div className="row" style={{ justifyContent: 'center', gap: '0.75rem', marginTop: '1rem' }}>
            <Link className="btn primary" to="/orders">View my orders</Link>
            <Link className="btn" to="/marketplace">Continue shopping</Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="page narrow">
      <h1>Checkout</h1>
      <Link className="muted" style={{ display: 'inline-block', marginBottom: '1rem' }} to="/marketplace">
        ← Back to marketplace
      </Link>

      <div className="card" style={{ marginBottom: '1.25rem' }}>
        <h3>{order.productTitle}</h3>
        <p className="muted">
          {order.providerName} · {order.productType} · Qty {order.quantity}
        </p>
        <table className="table">
          <tbody>
            <tr><td>Base</td><td className="currency">{order.currencyIso} {fmt(order.baseAmount)}</td></tr>
            <tr><td>Tax</td><td className="currency">{order.currencyIso} {fmt(order.taxAmount)}</td></tr>
            <tr><td>Service fee</td><td className="currency">{order.currencyIso} {fmt(order.serviceFee)}</td></tr>
            <tr><td>Discount</td><td className="currency">-{order.currencyIso} {fmt(order.discountAmount)}</td></tr>
            <tr>
              <td><strong>Total</strong></td>
              <td className="currency"><strong>{order.currencyIso} {fmt(order.totalAmount)}</strong></td>
            </tr>
          </tbody>
        </table>
        <p className="muted" style={{ marginTop: '0.5rem' }}>
          Order <span className="tag">{order.orderRef}</span> ·{' '}
          <span className="badge open">Pending payment</span>
        </p>
      </div>

      <div className="card">
        <h3>Payment details</h3>
        <form className="form" onSubmit={(e) => void submit(e)}>
          <div className="field">
            <label htmlFor="cardType">Card type</label>
            <select id="cardType" value={cardType} onChange={(e) => setCardType(e.target.value)}>
              {CARD_TYPES.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="field">
            <label htmlFor="cardName">Name on card</label>
            <input id="cardName" required value={cardName} onChange={(e) => setCardName(e.target.value)} placeholder="Full name" autoComplete="cc-name" />
          </div>
          <div className="field">
            <label htmlFor="cardNumber">Card number</label>
            <input id="cardNumber" required value={cardNumber} onChange={(e) => setCardNumber(e.target.value.replace(/\D/g, ''))} placeholder="4242 4242 4242 4242" inputMode="numeric" maxLength={16} autoComplete="cc-number" />
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="expiry">Expiry (MM/YY)</label>
              <input id="expiry" required value={expiry} onChange={(e) => setExpiry(e.target.value)} placeholder="MM/YY" maxLength={5} autoComplete="cc-exp" />
            </div>
            <div className="field">
              <label htmlFor="cvv">CVV</label>
              <input id="cvv" required type="password" value={cvv} onChange={(e) => setCvv(e.target.value.replace(/\D/g, ''))} placeholder="123" maxLength={4} autoComplete="cc-csc" />
            </div>
          </div>
          {payError && <p className="error">{payError}</p>}
          <button className="btn primary" type="submit" disabled={paying}>
            {paying ? 'Processing…' : `Pay ${order.currencyIso} ${fmt(order.totalAmount)}`}
          </button>
          <p className="muted" style={{ fontSize: '0.8rem' }}>
            Demo checkout — payment is simulated. No real card is charged.
          </p>
        </form>
      </div>
    </section>
  );
}
