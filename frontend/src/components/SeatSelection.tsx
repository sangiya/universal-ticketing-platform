import { useCallback, useEffect, useMemo, useState } from 'react';
import { Currency } from './UI';

/**
 * Seat map for bus / train / flight / event venues. Renders a realistic
 * seat-selection grid that handles:
 *   - 2-2 / 2-3 seat configurations
 *   - aisle separator
 *   - sold vs available vs selected
 *   - "double seat" pairing — adjacent seats share a faint border
 *   - boarding / dropping point dropdowns
 *   - live price calculation as seats are selected
 *   - seat guide & cancellation policy modal
 *
 * The component is purely UI — it does not hit any backend. The parent
 * receives the selected seat list and pricing via the callbacks.
 */

export type SeatStatus = 'available' | 'sold' | 'selected' | 'ladies' | 'disabled';

export interface Seat {
  id: string;        // e.g. "L-A1"
  row: number;       // 1..N
  col: number;       // 1..4 (or 1..5)
  side: 'left' | 'right';
  status: SeatStatus;
  /** "double-seat" pairing — A1+A2 share this id; useful for the "2-2" layout. */
  pairId?: string;
  /** optional fare multiplier (sleeper, premium, etc.) */
  fareMultiplier?: number;
  /** "Male" or "Female" — for the female-only seat block. */
  gender?: 'M' | 'F' | 'X';
  label?: string;    // e.g. "A1", "Aisle", "Driver", "Conductor"
}

export type SeatLayout = '2-2' | '2-3' | '3-2' | '1-1' | 'open';

export interface SeatSelectionProps {
  totalSeats?: number;          // default 40
  layout?: SeatLayout;           // default '2-2'
  seatPrice: number;
  currencyIso: string;
  productTitle: string;
  productType: string;
  reservedSeats?: string[];     // ids already sold
  femaleReservedSeats?: string[]; // sold to female customers (block selection by male)
  maxSelectable?: number;       // default 6
  /** When true, the meal picker is shown — used for trains, flights, long buses. */
  enableMeals?: boolean;
  meals?: MealOption[];
  onContinue: (selected: Seat[], totalPrice: number, mealSelections?: MealSelection[]) => void;
  onCancel?: () => void;
  /** Cancellable fare schedule — used to build the policies modal. */
  policies?: CancellationPolicy[];
}

export interface MealSelection {
  mealId: string;
  mealName: string;
  mealPrice: number;
  quantity: number;
  /** Optional — for "per-seat" meals, the seat the meal is for. */
  seatId?: string;
}

export interface CancellationPolicy {
  windowStartHours: number;       // hours before departure
  windowEndHours: number;         // hours before departure (or Infinity)
  label: string;
  sub?: string;                   // optional sub-label
  charge: string;                 // "5% Charge", "10% Charge", "Not Allowed"
  chargeKind: 'percent' | 'flat' | 'none';
  chargeValue: number;            // 5 / 10 / 0
  refundable: boolean;
  bg: string;                     // pastel bg color
  fg: string;                     // accent color
  badge: string;                  // "green"/"amber"/"red"/"blue"
  badgeBg: string;
  badgeFg: string;
}

export interface MealOption {
  id: string;            // "veg-thali", "chicken-biryani"
  name: string;
  description: string;
  price: number;
  category: 'VEG' | 'NON_VEG' | 'VEGAN' | 'SNACK' | 'BEVERAGE';
  emoji: string;
  available: boolean;
}

const DEFAULT_MEALS: MealOption[] = [
  { id: 'veg-thali', name: 'Veg Thali', description: 'Rice, dal, 2 sabzi, roti, pickle', price: 350, category: 'VEG', emoji: '🍛', available: true },
  { id: 'chicken-biryani', name: 'Chicken Biryani', description: 'Hyderabadi dum biryani with raita', price: 480, category: 'NON_VEG', emoji: '🍗', available: true },
  { id: 'veg-biryani', name: 'Veg Biryani', description: 'Aromatic basmati with vegetables', price: 380, category: 'VEG', emoji: '🍚', available: true },
  { id: 'masala-dosa', name: 'Masala Dosa', description: 'Crispy dosa with potato masala & chutney', price: 220, category: 'VEG', emoji: '🥞', available: true },
  { id: 'paneer-tikka', name: 'Paneer Tikka', description: 'Tandoori paneer with mint chutney', price: 380, category: 'VEG', emoji: '🧀', available: true },
  { id: 'mushroom-noodles', name: 'Mushroom Noodles', description: 'Stir-fried noodles with mushrooms', price: 280, category: 'VEG', emoji: '🍜', available: true },
  { id: 'samosa-combo', name: 'Samosa Combo', description: '2 samosas + green chutney + chai', price: 120, category: 'SNACK', emoji: '🥟', available: true },
  { id: 'cold-coffee', name: 'Cold Coffee', description: 'Chilled iced coffee', price: 150, category: 'BEVERAGE', emoji: '☕', available: true },
  { id: 'fruit-box', name: 'Fresh Fruit Box', description: 'Seasonal cut fruits', price: 180, category: 'VEGAN', emoji: '🍎', available: true },
  { id: 'chicken-roll', name: 'Chicken Roll', description: 'Spiced chicken wrapped in paratha', price: 250, category: 'NON_VEG', emoji: '🌯', available: true },
];

const DEFAULT_POLICIES: CancellationPolicy[] = [
  {
    windowStartHours: 72,
    windowEndHours: Number.POSITIVE_INFINITY,
    label: 'More than 72 Hours',
    sub: 'Prior to departure time',
    charge: '5% Charge',
    chargeKind: 'percent',
    chargeValue: 5,
    refundable: true,
    bg: '#d1fae5',
    fg: '#065f46',
    badge: 'green',
    badgeBg: '#10b981',
    badgeFg: '#ffffff',
  },
  {
    windowStartHours: 24,
    windowEndHours: 72,
    label: '24 - 72 Hours',
    sub: 'Prior to departure time',
    charge: '10% Charge',
    chargeKind: 'percent',
    chargeValue: 10,
    refundable: true,
    bg: '#fef3c7',
    fg: '#92400e',
    badge: 'amber',
    badgeBg: '#f59e0b',
    badgeFg: '#ffffff',
  },
  {
    windowStartHours: 0,
    windowEndHours: 24,
    label: 'Less than 24 Hours',
    sub: 'Non-refundable window',
    charge: 'Not Allowed',
    chargeKind: 'none',
    chargeValue: 0,
    refundable: false,
    bg: '#fee2e2',
    fg: '#991b1b',
    badge: 'red',
    badgeBg: '#ef4444',
    badgeFg: '#ffffff',
  },
  {
    windowStartHours: 0,
    windowEndHours: Number.POSITIVE_INFINITY,
    label: 'Festival tickets',
    sub: 'Non-refundable',
    charge: 'Not Allowed',
    chargeKind: 'none',
    chargeValue: 0,
    refundable: false,
    bg: '#dbeafe',
    fg: '#1e40af',
    badge: 'blue',
    badgeBg: '#1e3a8a',
    badgeFg: '#ffffff',
  },
];

const ROW_LABELS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N'];

/**
 * Generates a default seat map based on layout.
 *  - 2-2 = 4 seats per row, aisle between col 2 and col 3
 *  - 2-3 = 5 seats per row, aisle between col 2 and col 3
 *  - 1-1 = 2 seats per row, aisle between col 1 and col 2
 *  - open = no aisles
 */
function generateSeats(
  layout: SeatLayout,
  totalRows: number,
  reserved: string[],
  femaleReserved: string[],
  blockFemales: boolean,
): Seat[] {
  const cols: number = layout === '1-1' ? 2
    : layout === '2-2' || layout === '3-2' || layout === '2-3' ? (
      layout === '2-2' ? 4 : layout === '2-3' || layout === '3-2' ? 5 : 4
    )
    : 4;
  const seats: Seat[] = [];
  const sideBoundary = layout === '3-2' ? 3 : 2; // cols on left side
  for (let r = 1; r <= totalRows; r++) {
    const rowLetter = ROW_LABELS[r - 1] || `R${r}`;
    for (let c = 1; c <= cols; c++) {
      const id = `${rowLetter}${c}`;
      const isSold = reserved.includes(id);
      const isFemaleSold = femaleReserved.includes(id);
      const isLeft = c <= sideBoundary;
      const pairId = isLeft && c % 2 === 1 ? `${rowLetter}${c}-${rowLetter}${c + 1}` : undefined;
      let status: SeatStatus = 'available';
      if (isSold) status = 'sold';
      else if (isFemaleSold && blockFemales) status = 'ladies';
      seats.push({
        id,
        row: r,
        col: c,
        side: isLeft ? 'left' : 'right',
        status,
        pairId,
        gender: isFemaleSold ? 'F' : 'M',
      });
    }
  }
  return seats;
}

function findPair(seats: Seat[], seat: Seat): Seat | null {
  if (!seat.pairId) return null;
  return seats.find((s) => s.pairId && s.pairId === seat.pairId && s.id !== seat.id) || null;
}

export default function SeatSelection({
  totalSeats = 40,
  layout = '2-2',
  seatPrice,
  currencyIso,
  productTitle,
  productType,
  reservedSeats = [],
  femaleReservedSeats = [],
  maxSelectable = 6,
  enableMeals = false,
  meals = DEFAULT_MEALS,
  onContinue,
  onCancel,
  policies = DEFAULT_POLICIES,
}: SeatSelectionProps) {
  const totalRows = Math.max(1, Math.ceil(totalSeats / (layout === '2-3' || layout === '3-2' ? 5 : 4)));
  const [seats, setSeats] = useState<Seat[]>(() =>
    generateSeats(layout, totalRows, reservedSeats, femaleReservedSeats, true),
  );
  const [boarding, setBoarding] = useState<string>('');
  const [dropping, setDropping] = useState<string>('');
  const [showSeatGuide, setShowSeatGuide] = useState(false);
  const [showPolicies, setShowPolicies] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Meal selection state
  const [mealQty, setMealQty] = useState<Record<string, number>>({});
  const [mealCategory, setMealCategory] = useState<'ALL' | 'VEG' | 'NON_VEG' | 'VEGAN' | 'SNACK' | 'BEVERAGE'>('ALL');

  // Build list of unique origins / destinations from seat meta — for the
  // boarding / dropping dropdowns we use a simple city list. In a real app
  // these come from the trip endpoints; here we provide 5 standard stops.
  const stops = useMemo(() => [
    { time: '08:00', name: 'Origin Terminal' },
    { time: '08:30', name: 'Central Bus Stand' },
    { time: '09:00', name: 'Hill Top Bus Stand' },
    { time: '09:30', name: 'Highway Junction' },
    { time: '10:15', name: 'City Center Stop' },
    { time: '12:30', name: 'Riverside Stop' },
    { time: '14:00', name: 'Market Square' },
    { time: '15:30', name: 'Expressway Plaza' },
    { time: '17:00', name: 'Suburb Hub' },
    { time: '19:00', name: 'Destination Terminal' },
  ], []);

  // Reset seats when props change
  useEffect(() => {
    setSeats(generateSeats(layout, totalRows, reservedSeats, femaleReservedSeats, true));
  }, [layout, totalRows, reservedSeats, femaleReservedSeats]);

  const selectedSeats = useMemo(() => seats.filter((s) => s.status === 'selected'), [seats]);
  const totalPrice = useMemo(() => {
    const seatCost = selectedSeats.reduce(
      (sum, s) => sum + seatPrice * (s.fareMultiplier ?? 1),
      0,
    );
    const mealCost = Object.entries(mealQty).reduce(
      (sum, [id, qty]) => sum + (meals.find((m) => m.id === id)?.price ?? 0) * qty,
      0,
    );
    return seatCost + mealCost;
  }, [selectedSeats, seatPrice, mealQty, meals]);

  const totalMealCount = useMemo(
    () => Object.values(mealQty).reduce((s, q) => s + q, 0),
    [mealQty],
  );

  const toggleSeat = useCallback(
    (seat: Seat) => {
      if (seat.status === 'sold' || seat.status === 'disabled' || seat.status === 'ladies') {
        return;
      }
      setError(null);
      if (seat.status === 'selected') {
        // Deselect
        setSeats((prev) =>
          prev.map((s) => (s.id === seat.id ? { ...s, status: 'available' as SeatStatus } : s)),
        );
        return;
      }
      // Selecting — check cap
      if (selectedSeats.length >= maxSelectable) {
        setError(`You can select up to ${maxSelectable} seats at a time.`);
        return;
      }
      setSeats((prev) => prev.map((s) => (s.id === seat.id ? { ...s, status: 'selected' as SeatStatus } : s)));
    },
    [selectedSeats.length, maxSelectable],
  );

  // Render the seat grid row-by-row
  const renderRow = (rowNum: number) => {
    const rowSeats = seats.filter((s) => s.row === rowNum);
    const leftSeats = rowSeats.filter((s) => s.side === 'left');
    const rightSeats = rowSeats.filter((s) => s.side === 'right');
    return (
      <div key={rowNum} className="seat-row">
        <div className="seat-row-label">{ROW_LABELS[rowNum - 1] || rowNum}</div>
        <div className="seat-cluster">
          {leftSeats.map((s) => (
            <SeatCell key={s.id} seat={s} onClick={toggleSeat} pair={findPair(seats, s)} />
          ))}
        </div>
        <div className="seat-aisle" />
        <div className="seat-cluster">
          {rightSeats.map((s) => (
            <SeatCell key={s.id} seat={s} onClick={toggleSeat} pair={findPair(seats, s)} />
          ))}
        </div>
        <div className="seat-row-label">{ROW_LABELS[rowNum - 1] || rowNum}</div>
      </div>
    );
  };

  return (
    <div className="seat-picker">
      {/* ── Header strip with seat guide + policies ── */}
      <div className="seat-picker-header">
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{productTitle}</div>
          <div className="muted fs-xs">{productType} · from {currencyIso} {seatPrice.toFixed(2)} per seat</div>
        </div>
        <div className="seat-picker-actions">
          <button type="button" className="seat-pill" onClick={() => setShowSeatGuide(true)}>
            📖 Seat Guide
          </button>
          <span className="seat-pill-divider" />
          <button type="button" className="seat-pill" onClick={() => setShowPolicies(true)}>
            ⓘ Cancellation policy
          </button>
        </div>
      </div>

      {/* ── Boarding / Dropping selectors ── */}
      <div className="seat-stops">
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="seat-boarding">🟢 Boarding Point</label>
          <select
            id="seat-boarding"
            value={boarding}
            onChange={(e) => setBoarding(e.target.value)}
          >
            <option value="">Select boarding point</option>
            {stops.map((s) => (
              <option key={s.name} value={`${s.time} - ${s.name}`}>
                {s.time} - {s.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="seat-dropping">🔴 Dropping Point</label>
          <select
            id="seat-dropping"
            value={dropping}
            onChange={(e) => setDropping(e.target.value)}
          >
            <option value="">Select dropping point</option>
            {stops.slice().reverse().map((s) => (
              <option key={s.name} value={`${s.time} - ${s.name}`}>
                {s.time} - {s.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* ── Legend ── */}
      <div className="seat-legend">
        <div className="seat-legend-item">
          <span className="seat-cell-icon available" aria-hidden /> Available
        </div>
        <div className="seat-legend-item">
          <span className="seat-cell-icon sold" aria-hidden /> Sold
        </div>
        <div className="seat-legend-item">
          <span className="seat-cell-icon selected" aria-hidden /> Selected
        </div>
        <div className="seat-legend-item">
          <span className="seat-cell-icon ladies" aria-hidden /> Ladies
        </div>
      </div>

      {/* ── Seat map ── */}
      <div className="seat-map-wrap">
        <div className="seat-bus-icon" aria-hidden>🚌</div>
        <div className="seat-grid">
          {Array.from({ length: totalRows }, (_, i) => renderRow(i + 1))}
        </div>
        <div className="seat-driver" aria-hidden>🛞</div>
      </div>

      {error && (
        <div className="card" style={{ borderLeft: '3px solid var(--danger)', padding: '8px 12px', fontSize: '0.85rem' }}>
          ⚠️ {error}
        </div>
      )}

      {/* ── Meals / food picker ── */}
      {enableMeals && selectedSeats.length > 0 && (
        <div className="meal-picker">
          <div className="meal-picker-head">
            <div>
              <div className="meal-picker-title">🍱 Add meals (optional)</div>
              <div className="muted fs-xs">
                Pre-order food for your journey. Delivered to your seat at the next stop.
              </div>
            </div>
            {totalMealCount > 0 && (
              <span className="meal-pill">{totalMealCount} item{totalMealCount === 1 ? '' : 's'}</span>
            )}
          </div>

          <div className="meal-categories">
            {(['ALL', 'VEG', 'NON_VEG', 'VEGAN', 'SNACK', 'BEVERAGE'] as const).map((cat) => (
              <button
                key={cat}
                type="button"
                className={`meal-cat-chip ${mealCategory === cat ? 'active' : ''}`}
                onClick={() => setMealCategory(cat)}
              >
                {cat === 'ALL' && '🍽 All'}
                {cat === 'VEG' && '🥗 Veg'}
                {cat === 'NON_VEG' && '🍗 Non-Veg'}
                {cat === 'VEGAN' && '🌱 Vegan'}
                {cat === 'SNACK' && '🥨 Snacks'}
                {cat === 'BEVERAGE' && '🥤 Drinks'}
              </button>
            ))}
          </div>

          <div className="meal-list">
            {meals
              .filter((m) => mealCategory === 'ALL' || m.category === mealCategory)
              .map((m) => {
                const qty = mealQty[m.id] ?? 0;
                return (
                  <div key={m.id} className="meal-item">
                    <div className="meal-emoji">{m.emoji}</div>
                    <div className="meal-info">
                      <div className="meal-name">
                        {m.name}
                        {m.category === 'VEG' && <span className="dot veg" title="Veg" />}
                        {m.category === 'NON_VEG' && <span className="dot nonveg" title="Non-Veg" />}
                      </div>
                      <div className="muted fs-xs">{m.description}</div>
                    </div>
                    <div className="meal-price">
                      <Currency amount={m.price} currency={currencyIso} />
                    </div>
                    <div className="meal-qty">
                      <button
                        type="button"
                        className="meal-qty-btn"
                        onClick={() =>
                          setMealQty((prev) => ({ ...prev, [m.id]: Math.max(0, (prev[m.id] ?? 0) - 1) }))
                        }
                        disabled={qty === 0}
                        aria-label={`Remove one ${m.name}`}
                      >
                        −
                      </button>
                      <span className="meal-qty-num">{qty}</span>
                      <button
                        type="button"
                        className="meal-qty-btn"
                        onClick={() =>
                          setMealQty((prev) => ({ ...prev, [m.id]: Math.min(selectedSeats.length, (prev[m.id] ?? 0) + 1) }))
                        }
                        disabled={qty >= selectedSeats.length}
                        aria-label={`Add one ${m.name}`}
                      >
                        +
                      </button>
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      )}

      {/* ── Sticky footer: total + continue ── */}
      <div className="seat-footer">
        <div>
          <div className="muted fs-xs">Selected Seats ({selectedSeats.length})</div>
          <div className="seat-selected-list">
            {selectedSeats.length === 0
              ? <span className="muted">— none —</span>
              : selectedSeats.map((s) => (
                  <span key={s.id} className="seat-pill seat-pill-mini">{s.id}</span>
                ))}
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <div>
            <div className="muted fs-xs">Total</div>
            <Currency
              amount={totalPrice}
              currency={currencyIso}
              className="text-primary fw-800"
            />
          </div>
          <button
            type="button"
            className="btn primary"
            disabled={selectedSeats.length === 0}
            onClick={() => {
              const mealSelections: MealSelection[] = Object.entries(mealQty)
                .filter(([, qty]) => qty > 0)
                .map(([id, qty]) => {
                  const m = meals.find((x) => x.id === id)!;
                  return {
                    mealId: m.id,
                    mealName: m.name,
                    mealPrice: m.price,
                    quantity: qty,
                  };
                });
              onContinue(selectedSeats, totalPrice, mealSelections);
            }}
          >
            Continue →
          </button>
          {onCancel && (
            <button type="button" className="btn" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </div>

      {showSeatGuide && <SeatGuideModal onClose={() => setShowSeatGuide(false)} />}
      {showPolicies && (
        <PoliciesModal policies={policies} onClose={() => setShowPolicies(false)} />
      )}
    </div>
  );
}

function SeatCell({
  seat,
  pair,
  onClick,
}: {
  seat: Seat;
  pair: Seat | null;
  onClick: (s: Seat) => void;
}) {
  const status = seat.status;
  return (
    <button
      type="button"
      className={`seat-cell ${status} ${pair ? 'has-pair' : ''}`}
      onClick={() => onClick(seat)}
      disabled={status === 'sold' || status === 'disabled' || status === 'ladies'}
      aria-pressed={status === 'selected'}
      aria-label={`Seat ${seat.id} (${status})`}
      title={`${seat.id} — ${status}`}
    >
      <span className="seat-cell-icon" aria-hidden />
      <span className="seat-cell-label">{seat.id}</span>
    </button>
  );
}

function SeatGuideModal({ onClose }: { onClose: () => void }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>📖 Seat Guide</h3>
          <button className="link" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <ul className="seat-guide-list">
            <li><strong>Available</strong> — white outline. Click to select.</li>
            <li><strong>Sold</strong> — red, cannot be selected.</li>
            <li><strong>Selected</strong> — your chosen seats, highlighted in blue.</li>
            <li><strong>Ladies</strong> — booked by a female passenger; another female can still take the paired seat (double-seat booking) but male passengers cannot.</li>
            <li><strong>Pairing</strong> — adjacent seats (A1+A2, A3+A4) are linked so a couple can book together. The pairing is just a guide; you can also pick them individually.</li>
            <li><strong>Aisle</strong> — the gap between columns is the aisle. Don't sit there.</li>
            <li><strong>Boarding / Dropping</strong> — pick the closest stops so the operator can plan pickup.</li>
          </ul>
        </div>
        <div className="modal-foot">
          <button className="btn primary" onClick={onClose}>Got it</button>
        </div>
      </div>
    </div>
  );
}

function PoliciesModal({
  policies,
  onClose,
}: {
  policies: CancellationPolicy[];
  onClose: () => void;
}) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 460 }}>
        <div className="modal-head">
          <h3>Cancellation Policy</h3>
          <button className="link" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <div className="policy-list">
            {policies.map((p, i) => (
              <div
                key={i}
                className="policy-row"
                style={{ background: p.bg, color: p.fg }}
              >
                <div>
                  <div className="policy-label">{p.label}</div>
                  <div className="policy-sub">{p.sub}</div>
                </div>
                <div
                  className="policy-badge"
                  style={{ background: p.badgeBg, color: p.badgeFg }}
                >
                  {p.charge}
                </div>
              </div>
            ))}
            <div className="policy-note">
              <span aria-hidden style={{ marginRight: 4 }}>ⓘ</span>
              All applicable charges, including cancellation, convenience, payment
              processing, and other service fees, are based on the total fare and
              are non-refundable. By proceeding, you agree to the{' '}
              <a href="#">terms &amp; conditions</a>.
            </div>
          </div>
        </div>
        <div className="modal-foot">
          <button className="btn primary" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
