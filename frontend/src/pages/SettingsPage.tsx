import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface UserSettings {
  id?: number;
  theme: string;
  language: string;
  currency: string;
  notifyEmail: boolean;
  notifySms: boolean;
  notifyPush: boolean;
  notifyWhatsapp: boolean;
}

interface FamilyGroup {
  id: number;
  name: string;
  ownerUserId: number;
  memberCount: number;
  createdAt: string;
}

interface FamilyMember {
  userId: number;
  username: string;
  role: string;
  joinedAt: string;
}

interface Referral {
  id: number;
  code: string;
  status: string;
  inviteeEmailMasked: string | null;
  inviteeUserId: number | null;
  rewardPoints: number;
  createdAt: string;
  joinedAt: string | null;
}

interface PiiInfo {
  emailMasked: string;
  phoneMasked: string;
  identityDocMasked: string;
  piiProtected: boolean;
}

interface Traveler {
  id: number;
  fullName: string;
  relationship: string;
  dateOfBirth: string | null;
  gender: string;
  nationality: string | null;
  documentType: string | null;
  documentNumberMasked: string | null;
  phoneMasked: string | null;
  emailMasked: string | null;
  consentGiven: boolean;
  consentAt: string | null;
  createdAt: string;
}

interface SettingsForm {
  theme: string;
  language: string;
  currency: string;
  notifyEmail: boolean;
  notifySms: boolean;
  notifyPush: boolean;
  notifyWhatsapp: boolean;
}

const NOTIFY_FIELDS: { key: keyof SettingsForm; label: string }[] = [
  { key: 'notifyEmail', label: 'Email' },
  { key: 'notifySms', label: 'SMS' },
  { key: 'notifyPush', label: 'Push' },
  { key: 'notifyWhatsapp', label: 'WhatsApp' },
];

export default function SettingsPage() {
  const { api, authenticated } = useApi();

  const [settingsForm, setSettingsForm] = useState<SettingsForm>({
    theme: 'LIGHT',
    language: 'en',
    currency: 'LKR',
    notifyEmail: true,
    notifySms: false,
    notifyPush: true,
    notifyWhatsapp: false,
  });
  const [settingsLoading, setSettingsLoading] = useState(true);
  const [settingsError, setSettingsError] = useState<string | null>(null);
  const [settingsMessage, setSettingsMessage] = useState<string | null>(null);
  const [savingSettings, setSavingSettings] = useState(false);

  const [groups, setGroups] = useState<FamilyGroup[]>([]);
  const [groupsError, setGroupsError] = useState<string | null>(null);
  const [newGroupName, setNewGroupName] = useState('');
  const [joinFamilyId, setJoinFamilyId] = useState('');
  const [familyMessage, setFamilyMessage] = useState<string | null>(null);
  const [familyError, setFamilyError] = useState<string | null>(null);
  const [busyFamily, setBusyFamily] = useState(false);
  const [membersByGroup, setMembersByGroup] = useState<Record<number, FamilyMember[] | undefined>>({});
  const [expandedGroup, setExpandedGroup] = useState<number | null>(null);
  const [membersError, setMembersError] = useState<string | null>(null);
  const [loadingMembersId, setLoadingMembersId] = useState<number | null>(null);

  const [referrals, setReferrals] = useState<Referral[]>([]);
  const [referralsError, setReferralsError] = useState<string | null>(null);
  const [myCode, setMyCode] = useState<string | null>(null);
  const [inviteEmail, setInviteEmail] = useState('');
  const [referralMessage, setReferralMessage] = useState<string | null>(null);
  const [referralError, setReferralError] = useState<string | null>(null);
  const [busyReferral, setBusyReferral] = useState(false);

  const [travelers, setTravelers] = useState<Traveler[]>([]);
  const [travelersError, setTravelersError] = useState<string | null>(null);
  const [travelerMsg, setTravelerMsg] = useState<string | null>(null);
  const [busyTraveler, setBusyTraveler] = useState(false);
  const [travelerForm, setTravelerForm] = useState({ fullName: '', relationship: 'OTHER', dateOfBirth: '', gender: 'UNSPECIFIED', nationality: '', documentType: '', documentNumber: '', phone: '', email: '', consentGiven: false });

  const [pii, setPii] = useState<PiiInfo | null>(null);
  const [piiError, setPiiError] = useState<string | null>(null);

  const loadSettings = useCallback(async () => {
    setSettingsLoading(true);
    setSettingsError(null);
    try {
      const data = await api.get<UserSettings>('/settings');
      setSettingsForm({
        theme: data?.theme ?? 'LIGHT',
        language: data?.language ?? 'en',
        currency: data?.currency ?? 'LKR',
        notifyEmail: data?.notifyEmail ?? true,
        notifySms: data?.notifySms ?? false,
        notifyPush: data?.notifyPush ?? true,
        notifyWhatsapp: data?.notifyWhatsapp ?? false,
      });
    } catch (e) {
      setSettingsError(e instanceof Error ? e.message : 'Failed to load settings');
    } finally {
      setSettingsLoading(false);
    }
  }, [api]);

  const loadGroups = useCallback(async () => {
    setGroupsError(null);
    try {
      const data = await api.get<unknown[]>('/family');
      setGroups((data as unknown as FamilyGroup[]) ?? []);
    } catch (e) {
      setGroupsError(e instanceof Error ? e.message : 'Failed to load family groups');
    }
  }, [api]);

  const loadReferrals = useCallback(async () => {
    setReferralsError(null);
    try {
      const data = await api.get<unknown[]>('/referrals');
      const list = (data as unknown as Referral[]) ?? [];
      setReferrals(list);
      const active = list.find((r) => r.status === 'ACTIVE');
      if (active) setMyCode(active.code);
    } catch (e) {
      setReferralsError(e instanceof Error ? e.message : 'Failed to load referrals');
    }
  }, [api]);

  const loadPii = useCallback(async () => {
    setPiiError(null);
    try {
      const data = await api.get<PiiInfo>('/security/pii/me');
      setPii(data ?? null);
    } catch (e) {
      setPiiError(e instanceof Error ? e.message : 'Failed to load security status');
    }
  }, [api]);

  const loadTravelers = useCallback(async () => {
    setTravelersError(null);
    try {
      const data = await api.get<unknown[]>('/travelers');
      setTravelers((data as unknown as Traveler[]) ?? []);
    } catch (e) {
      setTravelersError(e instanceof Error ? e.message : 'Failed to load travelers');
    }
  }, [api]);

  useEffect(() => {
    if (!authenticated) return;
    void loadSettings();
    void loadGroups();
    void loadReferrals();
    void loadPii();
    void loadTravelers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const saveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingSettings(true);
    setSettingsMessage(null);
    setSettingsError(null);
    try {
      await api.put<unknown>('/settings', settingsForm);
      setSettingsMessage('Settings saved.');
      void loadSettings();
    } catch (err) {
      setSettingsError(err instanceof Error ? err.message : 'Failed to save settings');
    } finally {
      setSavingSettings(false);
    }
  };

  const createGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusyFamily(true);
    setFamilyMessage(null);
    setFamilyError(null);
    try {
      await api.post<unknown>('/family', { name: newGroupName });
      setFamilyMessage('Family group created.');
      setNewGroupName('');
      void loadGroups();
    } catch (err) {
      setFamilyError(err instanceof Error ? err.message : 'Failed to create group');
    } finally {
      setBusyFamily(false);
    }
  };

  const joinGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    const id = Number(joinFamilyId);
    if (!Number.isInteger(id) || id <= 0) {
      setFamilyError('Enter a valid family group ID.');
      return;
    }
    setBusyFamily(true);
    setFamilyMessage(null);
    setFamilyError(null);
    try {
      await api.post<unknown>(`/family/${id}/join`);
      setFamilyMessage('Joined the family group.');
      setJoinFamilyId('');
      void loadGroups();
    } catch (err) {
      setFamilyError(err instanceof Error ? err.message : 'Failed to join group');
    } finally {
      setBusyFamily(false);
    }
  };

  const toggleMembers = async (group: FamilyGroup) => {
    if (expandedGroup === group.id) {
      setExpandedGroup(null);
      return;
    }
    setExpandedGroup(group.id);
    setMembersError(null);
    if (membersByGroup[group.id]) return;
    setLoadingMembersId(group.id);
    try {
      const data = await api.get<unknown[]>(`/family/${group.id}/members`);
      setMembersByGroup((prev) => ({ ...prev, [group.id]: (data as unknown as FamilyMember[]) ?? [] }));
    } catch (e) {
      setMembersError(e instanceof Error ? e.message : 'Failed to load members');
    } finally {
      setLoadingMembersId(null);
    }
  };

  const createMyCode = async () => {
    setBusyReferral(true);
    setReferralMessage(null);
    setReferralError(null);
    try {
      const created = await api.post<Referral>('/referrals');
      if (created?.code) setMyCode(created.code);
      setReferralMessage('Your referral code is ready.');
      void loadReferrals();
    } catch (err) {
      setReferralError(err instanceof Error ? err.message : 'Failed to create referral code');
    } finally {
      setBusyReferral(false);
    }
  };

  const inviteFriend = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusyReferral(true);
    setReferralMessage(null);
    setReferralError(null);
    try {
      await api.post<unknown>('/referrals/invite', { email: inviteEmail });
      setReferralMessage(`Invite sent to ${inviteEmail}.`);
      setInviteEmail('');
      void loadReferrals();
    } catch (err) {
      setReferralError(err instanceof Error ? err.message : 'Failed to send invite');
    } finally {
      setBusyReferral(false);
    }
  };

  const copyCode = async () => {
    if (!myCode) return;
    try {
      await navigator.clipboard.writeText(myCode);
      setReferralMessage('Referral code copied to clipboard.');
    } catch {
      setReferralError('Could not copy code automatically. Select and copy it manually.');
    }
  };

  const createTraveler = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!travelerForm.consentGiven) { setTravelersError('Consent is required to store traveler data'); return; }
    setBusyTraveler(true);
    setTravelerMsg(null);
    setTravelersError(null);
    try {
      await api.post<unknown>('/travelers', {
        fullName: travelerForm.fullName,
        relationship: travelerForm.relationship,
        dateOfBirth: travelerForm.dateOfBirth || null,
        gender: travelerForm.gender,
        nationality: travelerForm.nationality || null,
        documentType: travelerForm.documentType || null,
        documentNumber: travelerForm.documentNumber || null,
        phone: travelerForm.phone || null,
        email: travelerForm.email || null,
        consentGiven: travelerForm.consentGiven
      });
      setTravelerMsg('Traveler saved — consent recorded');
      setTravelerForm({ fullName: '', relationship: 'OTHER', dateOfBirth: '', gender: 'UNSPECIFIED', nationality: '', documentType: '', documentNumber: '', phone: '', email: '', consentGiven: false });
      void loadTravelers();
    } catch (err) {
      setTravelersError(err instanceof Error ? err.message : 'Failed to save traveler');
    } finally { setBusyTraveler(false); }
  };

  const deleteTraveler = async (id: number) => {
    setBusyTraveler(true);
    try {
      const token = (api as unknown as { token: string | null }).token ?? localStorage.getItem('ticketmesh_token');
      const resp = await fetch(`/api/travelers/${id}`, { method: 'DELETE', headers: token ? { Authorization: `Bearer ${token}` } : {} });
      if (!resp.ok) throw new Error('Delete failed');
      setTravelerMsg('Traveler profile deleted — historical bookings preserved');
      void loadTravelers();
    } catch (err) {
      setTravelersError(err instanceof Error ? err.message : 'Failed to delete traveler');
    } finally { setBusyTraveler(false); }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <h1>Settings &amp; Social</h1>
        <p className="muted">
          Sign in to manage your preferences, family groups, referrals and
          security settings.
        </p>
        <Link className="btn primary" to="/login">
          Sign in
        </Link>
      </section>
    );
  }

  return (
    <section className="page">
      <h1>Settings &amp; Social</h1>
      <p className="muted">
        Preferences, family groups, referrals and security — all in one place.
      </p>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Settings</h2>
        {settingsLoading && <p className="muted">Loading your settings…</p>}
        {!settingsLoading && settingsError && <p className="error">{settingsError}</p>}
        {settingsMessage && <p className="success">{settingsMessage}</p>}
        <form className="form" onSubmit={(e) => void saveSettings(e)}>
          <div className="row">
            <div className="field">
              <label htmlFor="theme">Theme</label>
              <select
                id="theme"
                value={settingsForm.theme}
                onChange={(e) =>
                  setSettingsForm({ ...settingsForm, theme: e.target.value })
                }
              >
                <option value="LIGHT">Light</option>
                <option value="DARK">Dark</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="language">Language</label>
              <input
                id="language"
                value={settingsForm.language}
                onChange={(e) =>
                  setSettingsForm({ ...settingsForm, language: e.target.value })
                }
              />
            </div>
            <div className="field">
              <label htmlFor="currency">Currency (ISO-3)</label>
              <input
                id="currency"
                maxLength={3}
                value={settingsForm.currency}
                onChange={(e) =>
                  setSettingsForm({ ...settingsForm, currency: e.target.value.toUpperCase() })
                }
              />
            </div>
          </div>
          <fieldset className="field">
            <legend>Notifications</legend>
            {NOTIFY_FIELDS.map(({ key, label }) => (
              <label key={key} className="row" style={{ alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={Boolean(settingsForm[key])}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, [key]: e.target.checked })
                  }
                />
                {label}
              </label>
            ))}
          </fieldset>
          <button className="btn primary" type="submit" disabled={savingSettings || settingsLoading}>
            {savingSettings ? 'Saving…' : 'Save settings'}
          </button>
        </form>
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Saved Travelers</h2>
        <p className="muted">Manage passenger profiles. You must own the data or have explicit consent. Deleting a profile never alters historical bookings (retention).</p>
        {travelerMsg && <p className="success">{travelerMsg}</p>}
        {travelersError && <p className="error">{travelersError}</p>}
        <form className="form" onSubmit={(e) => void createTraveler(e)}>
          <div className="row">
            <div className="field" style={{ flex: 2 }}>
              <label htmlFor="travelerName">Full name *</label>
              <input id="travelerName" required value={travelerForm.fullName} onChange={(e) => setTravelerForm({ ...travelerForm, fullName: e.target.value })} placeholder="As on document" />
            </div>
            <div className="field">
              <label htmlFor="travelerRel">Relationship</label>
              <select id="travelerRel" value={travelerForm.relationship} onChange={(e) => setTravelerForm({ ...travelerForm, relationship: e.target.value })}>
                <option value="SELF">Self</option><option value="SPOUSE">Spouse</option><option value="CHILD">Child</option><option value="PARENT">Parent</option><option value="SIBLING">Sibling</option><option value="OTHER">Other</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="travelerDob">Date of birth</label>
              <input id="travelerDob" type="date" value={travelerForm.dateOfBirth} onChange={(e) => setTravelerForm({ ...travelerForm, dateOfBirth: e.target.value })} />
            </div>
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="travelerGender">Gender</label>
              <select id="travelerGender" value={travelerForm.gender} onChange={(e) => setTravelerForm({ ...travelerForm, gender: e.target.value })}>
                <option value="UNSPECIFIED">Unspecified</option><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="travelerNat">Nationality (ISO-2)</label>
              <input id="travelerNat" maxLength={2} value={travelerForm.nationality} onChange={(e) => setTravelerForm({ ...travelerForm, nationality: e.target.value.toUpperCase() })} placeholder="LK" />
            </div>
            <div className="field">
              <label htmlFor="travelerDocType">Document type</label>
              <select id="travelerDocType" value={travelerForm.documentType} onChange={(e) => setTravelerForm({ ...travelerForm, documentType: e.target.value })}>
                <option value="">—</option><option value="PASSPORT">Passport</option><option value="NIC">NIC</option><option value="DRIVING_LICENSE">Driving Licence</option><option value="OTHER">Other</option>
              </select>
            </div>
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="travelerDocNum">Document number</label>
              <input id="travelerDocNum" value={travelerForm.documentNumber} onChange={(e) => setTravelerForm({ ...travelerForm, documentNumber: e.target.value })} placeholder="Encrypted at rest" />
            </div>
            <div className="field">
              <label htmlFor="travelerPhone">Phone</label>
              <input id="travelerPhone" value={travelerForm.phone} onChange={(e) => setTravelerForm({ ...travelerForm, phone: e.target.value })} placeholder="+94..." />
            </div>
            <div className="field">
              <label htmlFor="travelerEmail">Email</label>
              <input id="travelerEmail" type="email" value={travelerForm.email} onChange={(e) => setTravelerForm({ ...travelerForm, email: e.target.value })} />
            </div>
          </div>
          <label className="row" style={{ alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem' }}>
            <input type="checkbox" checked={travelerForm.consentGiven} onChange={(e) => setTravelerForm({ ...travelerForm, consentGiven: e.target.checked })} />
            I confirm I have authority/consent to store this traveler’s data *
          </label>
          <button className="btn primary" type="submit" disabled={busyTraveler || !travelerForm.consentGiven || !travelerForm.fullName.trim()}>
            {busyTraveler ? 'Saving…' : 'Save traveler'}
          </button>
        </form>
        {travelers.length === 0 ? (
          <p className="muted" style={{ marginTop: '1rem' }}>No saved travelers yet.</p>
        ) : (
          <table className="table" style={{ marginTop: '1rem' }}>
            <thead><tr><th>Name</th><th>Relation</th><th>DOB</th><th>Document</th><th>Phone</th><th>Email</th><th>Consent</th><th></th></tr></thead>
            <tbody>
              {travelers.map((t) => (
                <tr key={t.id}>
                  <td>{t.fullName}</td>
                  <td><span className="tag">{t.relationship}</span></td>
                  <td>{t.dateOfBirth ? new Date(t.dateOfBirth).toLocaleDateString() : '—'}</td>
                  <td>{t.documentNumberMasked ?? '—'}</td>
                  <td>{t.phoneMasked ?? '—'}</td>
                  <td>{t.emailMasked ?? '—'}</td>
                  <td>{t.consentGiven ? <span className="badge confirmed">Yes</span> : <span className="badge blocked">No</span>}</td>
                  <td><button className="btn" style={{ fontSize: '0.8rem' }} onClick={() => void deleteTraveler(t.id)} disabled={busyTraveler}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Family</h2>
        <p className="muted">
          Group tickets and shared travel for you and the people you travel with.
        </p>
        {familyMessage && <p className="success">{familyMessage}</p>}
        {familyError && <p className="error">{familyError}</p>}
        <div className="row">
          <form className="field" onSubmit={(e) => void createGroup(e)} style={{ flex: 1 }}>
            <label htmlFor="newGroupName">Create a group</label>
            <input
              id="newGroupName"
              required
              value={newGroupName}
              onChange={(e) => setNewGroupName(e.target.value)}
            />
            <button className="btn primary" type="submit" disabled={busyFamily}>
              Create group
            </button>
          </form>
          <form className="field" onSubmit={(e) => void joinGroup(e)} style={{ flex: 1 }}>
            <label htmlFor="joinFamilyId">Join by ID</label>
            <input
              id="joinFamilyId"
              type="number"
              min={1}
              value={joinFamilyId}
              onChange={(e) => setJoinFamilyId(e.target.value)}
            />
            <button className="btn" type="submit" disabled={busyFamily}>
              Join group
            </button>
          </form>
        </div>
        {groupsError && <p className="error">{groupsError}</p>}
        {membersError && <p className="error">{membersError}</p>}
        {groups.length === 0 ? (
          <p className="muted">You are not part of any family group yet.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Members</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {groups.map((g) => (
                <FamilyGroupRow
                  key={g.id}
                  group={g}
                  expanded={expandedGroup === g.id}
                  loadingMembers={loadingMembersId === g.id}
                  members={membersByGroup[g.id]}
                  onToggle={() => void toggleMembers(g)}
                />
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Referrals &amp; Invite Friends</h2>
        <p className="muted">
          Share your code — every invited friend who joins earns you reward
          points.
        </p>
        {referralMessage && <p className="success">{referralMessage}</p>}
        {referralError && <p className="error">{referralError}</p>}
        {myCode ? (
          <div className="row" style={{ alignItems: 'center', margin: '1rem 0' }}>
            <span className="tag">{myCode}</span>
            <button className="btn" onClick={() => void copyCode()} disabled={busyReferral}>
              Copy code
            </button>
          </div>
        ) : (
          <button className="btn primary" onClick={() => void createMyCode()} disabled={busyReferral}>
            {busyReferral ? 'Creating…' : 'Create my referral code'}
          </button>
        )}
        <form className="form" onSubmit={(e) => void inviteFriend(e)}>
          <div className="field">
            <label htmlFor="inviteEmail">Invite a friend by email</label>
            <input
              id="inviteEmail"
              type="email"
              required
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
            />
          </div>
          <button className="btn primary" type="submit" disabled={busyReferral || !myCode}>
            Send invite
          </button>
        </form>
        {referralsError && <p className="error">{referralsError}</p>}
        {referrals.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Status</th>
                <th>Reward points</th>
                <th>Invitee</th>
                <th>Invited</th>
                <th>Joined</th>
              </tr>
            </thead>
            <tbody>
              {referrals.map((r) => (
                <tr key={r.id}>
                  <td>
                    <span className="tag">{r.code}</span>
                  </td>
                  <td>
                    <span className={`badge ${r.status.toLowerCase()}`}>{r.status}</span>
                  </td>
                  <td>{r.rewardPoints}</td>
                  <td>{r.inviteeEmailMasked ?? '—'}</td>
                  <td>{new Date(r.createdAt).toLocaleDateString()}</td>
                  <td>{r.joinedAt ? new Date(r.joinedAt).toLocaleDateString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h2>Security &amp; privacy</h2>
        <p className="muted">
          How we protect your sensitive personal information.
        </p>
        {piiError && <p className="error">{piiError}</p>}
        {pii ? (
          <>
            <div className="row" style={{ margin: '1rem 0' }}>
              <div className="field">
                <label>Email</label>
                <span>{pii.emailMasked}</span>
              </div>
              <div className="field">
                <label>Phone</label>
                <span>{pii.phoneMasked}</span>
              </div>
              <div className="field">
                <label>Identity document</label>
                <span>{pii.identityDocMasked}</span>
              </div>
            </div>
            <p>
              PII encryption:{' '}
              {pii.piiProtected ? (
                <span className="badge confirmed">Active</span>
              ) : (
                <span className="badge blocked">Inactive</span>
              )}
            </p>
          </>
        ) : (
          <p className="muted">Loading your security status…</p>
        )}
        <p className="muted" style={{ marginTop: '1rem' }}>
          Two-factor authentication and identity verification are available via
          the account security API. Reach out to support to enable them.
        </p>
      </div>
    </section>
  );
}

function FamilyGroupRow({
  group,
  expanded,
  loadingMembers,
  members,
  onToggle,
}: {
  group: FamilyGroup;
  expanded: boolean;
  loadingMembers: boolean;
  members: FamilyMember[] | undefined;
  onToggle: () => void;
}) {
  return (
    <>
      <tr>
        <td>{group.name}</td>
        <td>{group.memberCount}</td>
        <td>{new Date(group.createdAt).toLocaleDateString()}</td>
        <td>
          <button className="btn" onClick={onToggle}>
            {expanded ? 'Hide members' : 'Members'}
          </button>
        </td>
      </tr>
      {expanded && (
        <tr key={`${group.id}-members`}>
          <td colSpan={4}>
            {loadingMembers && <p className="muted">Loading members…</p>}
            {!loadingMembers && (!members || members.length === 0) && (
              <p className="muted">No members yet.</p>
            )}
            {!loadingMembers && members && members.length > 0 && (
              <table className="table">
                <thead>
                  <tr>
                    <th>User ID</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th>Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {members.map((m) => (
                    <tr key={m.userId}>
                      <td>{m.userId}</td>
                      <td>{m.username}</td>
                      <td>
                        <span className="tag">{m.role}</span>
                      </td>
                      <td>{new Date(m.joinedAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </td>
        </tr>
      )}
    </>
  );
}
