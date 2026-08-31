import { useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link, useNavigate } from 'react-router-dom';

interface Offer {
  id: number;
  title: string;
  providerName: string;
  providerCode?: string;
  price: string | number;
  currency: string;
  currencyIso?: string;
  productType?: string;
  themeColor?: string | null;
  origin?: string | null;
  destination?: string | null;
}

const HERO_TABS = ['Bus','Train','Flights','Movies','Events','More'];
const POPULAR = [
  { icon:'🚌', label:'Bus Tickets', sub:'Search & Book Bus', count:'124 services' },
  { icon:'🚆', label:'Train Tickets', sub:'Book Train Tickets', count:'86 routes' },
  { icon:'🎬', label:'Movie Tickets', sub:'Get Show Tickets', count:'240 shows' },
  { icon:'🎟️', label:'Event Tickets', sub:'Concerts & Sports', count:'58 events' },
  { icon:'🏖️', label:'Attractions', sub:'Tours & Activities', count:'42 tours' },
];

export default function HomePage() {
  const { api, authenticated } = useApi();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [from, setFrom] = useState('Sydney, Australia');
  const [to, setTo] = useState('Melbourne, Australia');
  const [date, setDate] = useState('2024-05-24');
  const [passengers, setPassengers] = useState('1 Passenger');
  const [activeTab, setActiveTab] = useState('Bus');
  const [offers, setOffers] = useState<Offer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<{liveOffers:number,verticals:number,portals:number,availability:string}|null>(null);
  const [wallet, setWallet] = useState<number | null>(null);
  const [recent, setRecent] = useState<string[]>(()=>{ try{ return JSON.parse(localStorage.getItem('tm_recent')||'[]'); }catch{ return []; }});

  const search = async (q = query) => {
    setLoading(true); setError(null);
    try {
      const endpoint = `/catalog/search${q ? `?q=${encodeURIComponent(q)}` : ''}`;
      const data = await api.get<unknown[]>(endpoint);
      setOffers((data as unknown as Offer[]) ?? []);
      if (q) { const r = [q, ...recent.filter(x=>x!==q)].slice(0,5); setRecent(r); localStorage.setItem('tm_recent', JSON.stringify(r)); }
    } catch (e) { setError(e instanceof Error ? e.message : 'Search failed'); setOffers([]); }
    finally { setLoading(false); }
  };

  useEffect(()=>{ void search(''); api.get<any>('/public/stats').then(setStats).catch(()=>{}); if(authenticated) api.get<any>('/loyalty').then(d=>setWallet(d?.points ?? null)).catch(()=>{}); }, [authenticated]);

  const consoleSearch = () => {
    const q = [from, to, activeTab].filter(Boolean).join(' ');
    setQuery(q); void search(q);
  };

  const filtered = activeTab==='More'||activeTab==='All' ? offers : offers.filter(o=> (o.productType??'').toLowerCase().includes(activeTab.toLowerCase().slice(0,4)) || (o.title??'').toLowerCase().includes(activeTab.toLowerCase()));
  const topRoutes = offers.slice(0,4);
  const coverColor = (o:Offer)=> o.themeColor || '#4f46e5';
  const book = ()=> authenticated ? navigate('/marketplace') : navigate('/login');

  return (
    <section className="page">
      <div className="hero" style={{ paddingBottom:'2.2rem'}}>
        <span className="eyebrow">One Platform. All Ticket Services. Anywhere.</span>
        <h1>Book Your Journey<br/>Anywhere in the World</h1>
        <p className="lead">TicketMesh is a global, multi-service ticketing ecosystem that connects customers, agents, providers and administrators on one powerful platform.</p>

        <div className="search-console">
          <div className="field"><label>From</label><input value={from} onChange={e=>setFrom(e.target.value)} placeholder="Sydney, Australia" /></div>
          <div className="field"><label>To</label><input value={to} onChange={e=>setTo(e.target.value)} placeholder="Melbourne, Australia" /></div>
          <div className="field"><label>Date</label><input type="date" value={date} onChange={e=>setDate(e.target.value)} /></div>
          <div className="field"><label>Passengers</label><select value={passengers} onChange={e=>setPassengers(e.target.value)}><option>1 Passenger</option><option>2 Passengers</option><option>3 Passengers</option><option>4 Passengers</option></select></div>
          <button className="btn primary" style={{ height:46, borderRadius:10, fontWeight:800 }} onClick={consoleSearch}>Search</button>
        </div>

        <div className="hero-tabs" style={{ marginTop:'1.1rem'}}>
          {HERO_TABS.map(t=> <button key={t} className={`hero-tab ${activeTab===t?'active':''}`} onClick={()=>setActiveTab(t)}>{t}</button>)}
        </div>

        <div style={{ display:'grid', gridTemplateColumns: authenticated ? '1fr 280px' : '1fr', gap:'1.2rem', maxWidth:980, margin:'1.4rem auto 0' }}>
          <div style={{ display:'flex', gap:'0.75rem', flexWrap:'wrap', alignItems:'center', color:'rgba(255,255,255,0.9)', fontSize:'0.82rem' }}>
            <span>✦ Best Price Guarantee</span><span>•</span><span>24/7 Customer Support</span><span>•</span><span>Secure Payments</span><span>•</span><span>Instant Confirmation</span>
          </div>
          {authenticated && (
            <div className="wallet-card">
              <small>Wallet Balance</small><strong style={{ fontSize:'1.35rem'}}>AUD {wallet ?? 120.50}</strong><span style={{ fontSize:'0.78rem', opacity:0.9}}>+ Add Money</span>
            </div>
          )}
        </div>
      </div>

      {recent.length>0 && (
        <div style={{ maxWidth:980, margin:'0 auto 1rem', background:'#fff', border:'1px solid var(--border)', borderRadius:12, padding:'0.9rem 1.1rem' }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}><strong style={{ fontSize:'0.9rem'}}>Recent Searches</strong><button className="link" style={{ fontSize:'0.8rem'}} onClick={()=>{ setRecent([]); localStorage.removeItem('tm_recent');}}>Clear All</button></div>
          <div style={{ display:'flex', gap:'1rem', marginTop:'0.6rem', flexWrap:'wrap'}}>
            {recent.map(r=> <span key={r} className="tag" style={{ cursor:'pointer'}} onClick={()=>{ setQuery(r); void search(r);}}>{r}</span>)}
          </div>
        </div>
      )}

      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', margin:'1.2rem 0 0.6rem'}}>
        <h2 className="section-title" style={{ margin:0}}>Popular Services</h2><Link className="muted" to="/marketplace" style={{ fontSize:'0.85rem'}}>View All</Link>
      </div>
      <div className="shelf">
        {POPULAR.map(p=> (
          <div key={p.label} className="shelf-card" onClick={()=>{ setActiveTab(p.label.split(' ')[0]); void search(p.label.split(' ')[0]);}} style={{ cursor:'pointer'}}>
            <div className="ico">{p.icon}</div><strong style={{ fontSize:'0.92rem'}}>{p.label}</strong><div className="muted" style={{ fontSize:'0.78rem'}}>{p.sub}</div><div className="muted" style={{ fontSize:'0.72rem', marginTop:'0.35rem'}}>{p.count}</div>
          </div>
        ))}
      </div>

      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', margin:'1.2rem 0 0.6rem'}}>
        <h2 className="section-title" style={{ margin:0}}>Top Routes</h2><span className="muted" style={{ fontSize:'0.82rem'}}>{query? `for "${query}"` : 'From AUD 25.00'}</span>
      </div>
      <div className="top-routes">
        {topRoutes.map(o=> (
          <div key={o.id} className="route-card">
            <div style={{ display:'flex', gap:'0.8rem', alignItems:'center'}}>
              <div style={{ width:36, height:36, borderRadius:8, background:coverColor(o), color:'#fff', display:'flex', alignItems:'center', justifyContent:'center'}}>✈</div>
              <div><strong>{o.origin && o.destination ? `${o.origin} → ${o.destination}` : o.title}</strong><div className="muted" style={{ fontSize:'0.78rem'}}>{o.providerName} · {o.productType ?? 'TICKET'}</div></div>
            </div>
            <div style={{ textAlign:'right'}}><strong style={{ color:coverColor(o)}}>{o.currencyIso ?? o.currency} {o.price}</strong><div className="muted" style={{ fontSize:'0.72rem'}}>1 Passenger</div></div>
          </div>
        ))}
        {topRoutes.length===0 && <p className="muted">No routes — try a different search.</p>}
      </div>

      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', margin:'1.6rem 0 0.6rem'}}>
        <h2 className="section-title" style={{ margin:0}}>Live Offers</h2>
        <div className="hero-stats" style={{ background:'transparent', padding:0, gap:'1.1rem'}}>
          <div className="hero-stat"><span className="num" style={{ color:'var(--primary)'}}>{stats? stats.liveOffers : '…'}</span> <span className="lab" style={{ color:'var(--muted)'}}>live offers</span></div>
          <div className="hero-stat"><span className="num" style={{ color:'var(--primary)'}}>{stats? stats.verticals: '…'}</span> <span className="lab">verticals</span></div>
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Searching the marketplace…</p>}
      <div className="grid">
        {filtered.map(o=> (
          <article className="card product-card" key={o.id}>
            <div className="product-cover" style={{ background:`linear-gradient(135deg, ${coverColor(o)}, #7c3aed)`}}>
              <div className="cover-tint" />
              <span className="product-type">{o.productType ?? o.providerCode ?? 'Offer'}</span>
              <span className="price-badge">{(o.currency ?? (o as any).currencyIso ?? 'LKR')} {o.price}</span>
            </div>
            <div className="product-body">
              <h3>{o.title}</h3>
              <p className="muted" style={{ fontSize:'0.85rem'}}>by <strong style={{ color:coverColor(o)}}>{o.providerName}</strong>{o.origin && o.destination ? ` · ${o.origin} → ${o.destination}`:''}</p>
              <button className="btn primary" onClick={book}>{authenticated?'Book now':'Sign in to book'}</button>
            </div>
          </article>
        ))}
        {!loading && filtered.length===0 && <p className="muted">No results. Try a different search.</p>}
      </div>

      <div className="testimonials" style={{ marginTop:'2.2rem'}}>
        <div className="testimonial"><p className="quote">“Everything I need in one place — I booked a rail pass, a concert and a city tour without leaving the app.”</p><div className="who"><div className="avatar">A</div><div><strong>Amaya Silva</strong><span>Frequent traveller</span></div></div></div>
        <div className="testimonial"><p className="quote">“The seller portal is genuinely production-grade. My providers, products and branding all sit in one dashboard.”</p><div className="who"><div className="avatar">D</div><div><strong>Dinesh Fernando</strong><span>Tour operator</span></div></div></div>
      </div>

      <div className="banner">
        <h2>Want to sell your services?</h2>
        <p className="muted">Agents and shops connect via the app, upload their services and start selling. Your shop, your theme, your brand.</p>
        <Link className="btn primary" to="/register">Become an agent / start a shop</Link>
      </div>
    </section>
  );
}
