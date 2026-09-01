import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import { detectLocale } from '../utils/locale';
import {
  Alert,
  EmptyState,
  PageHeader,
  SectionTitle,
  Skeleton,
  StatCard,
} from '../components/UI';
import { useToast } from '../components/Toast';

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

const NOTIFY_FIELDS: {
  key: keyof SettingsForm;
  label: string;
  icon: string;
  description: string;
}[] = [
  { key: 'notifyEmail', label: 'Email', icon: '📧', description: 'Receipts, confirmations, order updates' },
  { key: 'notifySms', label: 'SMS', icon: '📱', description: 'Time-sensitive alerts' },
  { key: 'notifyPush', label: 'Push', icon: '🔔', description: 'In-app and browser push' },
  { key: 'notifyWhatsapp', label: 'WhatsApp', icon: '💬', description: 'Travel reminders on WhatsApp' },
];

const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'si', label: 'සිංහල' },
  { code: 'ta', label: 'தமிழ்' },
  { code: 'hi', label: 'हिन्दी' },
  { code: 'fr', label: 'Français' },
  { code: 'es', label: 'Español' },
  { code: 'de', label: 'Deutsch' },
  { code: 'zh', label: '中文' },
  { code: 'ja', label: '日本語' },
  { code: 'ar', label: 'العربية' },
];

const CURRENCIES = ['USD', 'EUR', 'GBP', 'LKR', 'INR', 'AUD', 'SGD', 'JPY'];

const RELATIONSHIPS = [
  { key: 'SELF', icon: '👤' },
  { key: 'SPOUSE', icon: '💑' },
  { key: 'CHILD', icon: '🧒' },
  { key: 'PARENT', icon: '👵' },
  { key: 'SIBLING', icon: '👫' },
  { key: 'OTHER', icon: '👥' },
];

const GENDERS = [
  { key: 'UNSPECIFIED', label: 'Unspecified' },
  { key: 'MALE', label: 'Male' },
  { key: 'FEMALE', label: 'Female' },
  { key: 'OTHER', label: 'Other' },
];

const DOC_TYPES = [
  { key: '', label: '—' },
  { key: 'PASSPORT', label: 'Passport' },
  { key: 'NIC', label: 'NIC' },
  { key: 'DRIVING_LICENSE', label: 'Driving Licence' },
  { key: 'OTHER', label: 'Other' },
];

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'active' || v === 'joined' || v === 'rewarded' || v === 'confirmed') return 'success';
  if (v === 'pending' || v === 'invited' || v === 'in_progress') return 'warn';
  if (v === 'expired' || v === 'cancelled' || v === 'revoked') return 'danger';
  return 'info';
}

const LOCALE_DEFAULTS = detectLocale();

export default function SettingsPage() {
  const { api, authenticated } = useApi();
  const { push } = useToast();

  const [settingsForm, setSettingsForm] = useState<SettingsForm>({
    theme: 'LIGHT',
    language: LOCALE_DEFAULTS.language,
    currency: LOCALE_DEFAULTS.currencyIso,
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
  const [membersByGroup, setMembersByGroup] = useState<
    Record<number, FamilyMember[] | undefined>
  >({});
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
  const [travelerForm, setTravelerForm] = useState({
    fullName: '',
    relationship: 'OTHER',
    dateOfBirth: '',
    gender: 'UNSPECIFIED',
    nationality: '',
    documentType: '',
    documentNumber: '',
    phone: '',
    email: '',
    consentGiven: false,
  });

  const [pii, setPii] = useState<PiiInfo | null>(null);
  const [piiError, setPiiError] = useState<string | null>(null);

  const loadSettings = useCallback(async () => {
    setSettingsLoading(true);
    setSettingsError(null);
    try {
      const data = await api.get<UserSettings>('/settings');
      setSettingsForm({
        theme: data?.theme ?? 'LIGHT',
        language: data?.language ?? LOCALE_DEFAULTS.language,
        currency: data?.currency ?? LOCALE_DEFAULTS.currencyIso,
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
      push('Settings saved', 'success');
      void loadSettings();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to save settings';
      setSettingsError(msg);
      push(msg, 'error');
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
      push(`Group "${newGroupName}" created`, 'success');
      setNewGroupName('');
      void loadGroups();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to create group';
      setFamilyError(msg);
      push(msg, 'error');
    } finally {
      setBusyFamily(false);
    }
  };

  const joinGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    const id = Number(joinFamilyId);
    if (!Number.isInteger(id) || id <= 0) {
      setFamilyError('Enter a valid family group ID.');
      push('Enter a valid family group ID', 'warn');
      return;
    }
    setBusyFamily(true);
    setFamilyMessage(null);
    setFamilyError(null);
    try {
      await api.post<unknown>(`/family/${id}/join`);
      setFamilyMessage('Joined the family group.');
      push('Joined the family group', 'success');
      setJoinFamilyId('');
      void loadGroups();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to join group';
      setFamilyError(msg);
      push(msg, 'error');
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
      setMembersByGroup((prev) => ({
        ...prev,
        [group.id]: (data as unknown as FamilyMember[]) ?? [],
      }));
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
      push('Your referral code is ready', 'success');
      void loadReferrals();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to create referral code';
      setReferralError(msg);
      push(msg, 'error');
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
      push(`Invite sent to ${inviteEmail}`, 'success');
      setInviteEmail('');
      void loadReferrals();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to send invite';
      setReferralError(msg);
      push(msg, 'error');
    } finally {
      setBusyReferral(false);
    }
  };

  const copyCode = async () => {
    if (!myCode) return;
    try {
      await navigator.clipboard.writeText(myCode);
      setReferralMessage('Referral code copied to clipboard.');
      push('Copied to clipboard', 'success');
    } catch {
      setReferralError(
        'Could not copy code automatically. Select and copy it manually.'
      );
      push('Could not copy code', 'error');
    }
  };

  const createTraveler = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!travelerForm.consentGiven) {
      setTravelersError('Consent is required to store traveler data');
      push('Consent is required to store traveler data', 'warn');
      return;
    }
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
        consentGiven: travelerForm.consentGiven,
      });
      setTravelerMsg('Traveler saved — consent recorded.');
      push('Traveler saved', 'success');
      setTravelerForm({
        fullName: '',
        relationship: 'OTHER',
        dateOfBirth: '',
        gender: 'UNSPECIFIED',
        nationality: '',
        documentType: '',
        documentNumber: '',
        phone: '',
        email: '',
        consentGiven: false,
      });
      void loadTravelers();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to save traveler';
      setTravelersError(msg);
      push(msg, 'error');
    } finally {
      setBusyTraveler(false);
    }
  };

  const deleteTraveler = async (id: number) => {
    setBusyTraveler(true);
    try {
      await api.delete(`/travelers/${id}`);
      setTravelerMsg('Traveler profile deleted — historical bookings preserved.');
      push('Traveler deleted', 'success');
      void loadTravelers();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to delete traveler';
      setTravelersError(msg);
      push(msg, 'error');
    } finally {
      setBusyTraveler(false);
    }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <PageHeader
          title="Settings & Social"
          subtitle="Manage your preferences, family groups, referrals and security settings."
        />
        <EmptyState
          icon="⚙️"
          title="Sign in to access settings"
          description="All your preferences, family groups, and security controls live here."
          action={
            <Link to="/login" className="btn primary">
              Sign in
            </Link>
          }
        />
      </section>
    );
  }

  const totalMembers = groups.reduce((sum, g) => sum + g.memberCount, 0);
  const totalRewardPoints = referrals.reduce((sum, r) => sum + r.rewardPoints, 0);
  const activeReferrals = referrals.filter((r) => r.status === 'ACTIVE' || r.status === 'JOINED').length;

  return (
    <section className="page">
      <PageHeader
        title="⚙️ Settings & Social"
        subtitle="Preferences, family groups, referrals, travelers and security — all in one place."
      />

      <div className="stats">
        <StatCard label="Travelers" value={travelers.length} icon="🧳" variant="info" />
        <StatCard label="Family groups" value={groups.length} icon="👨‍👩‍👧" variant="violet" />
        <StatCard label="Family members" value={totalMembers} icon="👥" variant="warning" />
        <StatCard
          label="Reward points"
          value={totalRewardPoints}
          icon="⭐"
          variant="success"
        />
        <StatCard
          label="Active referrals"
          value={activeReferrals}
          icon="🎁"
          variant="success"
        />
        <StatCard
          label="PII protection"
          value={pii?.piiProtected ? 'Active' : '—'}
          icon="🛡"
          variant={pii?.piiProtected ? 'success' : 'warning'}
        />
      </div>

      {settingsError && <Alert kind="danger">{settingsError}</Alert>}
      {settingsMessage && <Alert kind="success">{settingsMessage}</Alert>}

      <SectionTitle
        title="🎨 Preferences"
        subtitle="Personalize your experience and how we communicate with you."
      />
      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        {settingsLoading ? (
          <Skeleton lines={5} />
        ) : (
          <form className="form" onSubmit={(e) => void saveSettings(e)}>
            <div className="row">
              <div className="field">
                <label htmlFor="theme">Theme</label>
                <select
                  id="theme"
                  value={settingsForm.theme}
                  onChange={(e) => setSettingsForm({ ...settingsForm, theme: e.target.value })}
                >
                  <option value="LIGHT">☀️ Light</option>
                  <option value="DARK">🌙 Dark</option>
                </select>
              </div>
              <div className="field">
                <label htmlFor="language">Language</label>
                <select
                  id="language"
                  value={settingsForm.language}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, language: e.target.value })
                  }
                >
                  {LANGUAGES.map((l) => (
                    <option key={l.code} value={l.code}>
                      {l.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="currency">Default currency</label>
                <select
                  id="currency"
                  value={settingsForm.currency}
                  onChange={(e) => setSettingsForm({ ...settingsForm, currency: e.target.value })}
                >
                  {CURRENCIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="field">
              <label style={{ marginBottom: 'var(--space-2)' }}>Notification channels</label>
              <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
                {NOTIFY_FIELDS.map(({ key, label, icon, description }) => (
                  <label
                    key={key}
                    className="card compact"
                    style={{
                      cursor: 'pointer',
                      borderColor: settingsForm[key] ? 'var(--primary)' : undefined,
                      background: settingsForm[key] ? 'var(--gradient-soft)' : undefined,
                    }}
                  >
                    <div className="row tight" style={{ alignItems: 'flex-start' }}>
                      <input
                        type="checkbox"
                        checked={Boolean(settingsForm[key])}
                        onChange={(e) =>
                          setSettingsForm({ ...settingsForm, [key]: e.target.checked })
                        }
                        style={{ marginTop: 4 }}
                      />
                      <div>
                        <strong style={{ fontSize: '0.9rem' }}>
                          {icon} {label}
                        </strong>
                        <p
                          className="muted"
                          style={{ margin: '0.2rem 0 0', fontSize: '0.75rem' }}
                        >
                          {description}
                        </p>
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            </div>
            <button
              className="btn primary"
              type="submit"
              disabled={savingSettings || settingsLoading}
            >
              {savingSettings ? (
                <>
                  <span className="spinner" />
                  Saving…
                </>
              ) : (
                '💾 Save preferences'
              )}
            </button>
          </form>
        )}
      </div>

      <SectionTitle
        title="🧳 Saved travelers"
        subtitle="Manage passenger profiles. Deleting a profile never alters historical bookings."
      />
      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        {travelerMsg && <Alert kind="success">{travelerMsg}</Alert>}
        {travelersError && <Alert kind="danger">{travelersError}</Alert>}

        <form className="form" onSubmit={(e) => void createTraveler(e)}>
          <div className="row">
            <div className="field" style={{ flex: 2 }}>
              <label htmlFor="travelerName">
                Full name <span className="req">*</span>
              </label>
              <input
                id="travelerName"
                required
                value={travelerForm.fullName}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, fullName: e.target.value })
                }
                placeholder="As on document"
              />
            </div>
            <div className="field">
              <label htmlFor="travelerRel">Relationship</label>
              <select
                id="travelerRel"
                value={travelerForm.relationship}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, relationship: e.target.value })
                }
              >
                {RELATIONSHIPS.map((r) => (
                  <option key={r.key} value={r.key}>
                    {r.icon} {r.key}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="travelerDob">Date of birth</label>
              <input
                id="travelerDob"
                type="date"
                value={travelerForm.dateOfBirth}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, dateOfBirth: e.target.value })
                }
              />
            </div>
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="travelerGender">Gender</label>
              <select
                id="travelerGender"
                value={travelerForm.gender}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, gender: e.target.value })
                }
              >
                {GENDERS.map((g) => (
                  <option key={g.key} value={g.key}>
                    {g.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="travelerNat">Nationality (ISO-2)</label>
              <input
                id="travelerNat"
                maxLength={2}
                value={travelerForm.nationality}
                onChange={(e) =>
                  setTravelerForm({
                    ...travelerForm,
                    nationality: e.target.value.toUpperCase(),
                  })
                }
                placeholder={LOCALE_DEFAULTS.countryIso || 'XX'}
              />
            </div>
            <div className="field">
              <label htmlFor="travelerDocType">Document type</label>
              <select
                id="travelerDocType"
                value={travelerForm.documentType}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, documentType: e.target.value })
                }
              >
                {DOC_TYPES.map((d) => (
                  <option key={d.key} value={d.key}>
                    {d.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="travelerDocNum">Document number</label>
              <input
                id="travelerDocNum"
                value={travelerForm.documentNumber}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, documentNumber: e.target.value })
                }
                placeholder="Encrypted at rest"
              />
            </div>
            <div className="field">
              <label htmlFor="travelerPhone">Phone</label>
              <input
                id="travelerPhone"
                value={travelerForm.phone}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, phone: e.target.value })
                }
                placeholder="+94…"
              />
            </div>
            <div className="field">
              <label htmlFor="travelerEmail">Email</label>
              <input
                id="travelerEmail"
                type="email"
                value={travelerForm.email}
                onChange={(e) =>
                  setTravelerForm({ ...travelerForm, email: e.target.value })
                }
              />
            </div>
          </div>
          <label
            className="row tight"
            style={{ alignItems: 'flex-start', gap: '0.5rem', marginTop: '0.5rem' }}
          >
            <input
              type="checkbox"
              checked={travelerForm.consentGiven}
              onChange={(e) =>
                setTravelerForm({ ...travelerForm, consentGiven: e.target.checked })
              }
              style={{ marginTop: 4 }}
            />
            <span className="fs-sm">
              I confirm I have authority/consent to store this traveler's data.{' '}
              <span className="req">*</span>
            </span>
          </label>
          <button
            className="btn primary"
            type="submit"
            disabled={
              busyTraveler || !travelerForm.consentGiven || !travelerForm.fullName.trim()
            }
          >
            {busyTraveler ? (
              <>
                <span className="spinner" />
                Saving…
              </>
            ) : (
              '💾 Save traveler'
            )}
          </button>
        </form>

        {travelers.length === 0 ? (
          <EmptyState
            icon="🧳"
            title="No saved travelers"
            description="Add a traveler above to speed up future bookings."
          />
        ) : (
          <div className="grid" style={{ marginTop: 'var(--space-4)' }}>
            {travelers.map((t) => {
              const rel = RELATIONSHIPS.find((r) => r.key === t.relationship);
              return (
                <article className="card compact" key={t.id}>
                  <div className="row between center" style={{ marginBottom: 'var(--space-2)' }}>
                    <strong>
                      {rel?.icon} {t.fullName}
                    </strong>
                    <span className={`badge ${t.consentGiven ? 'success' : 'danger'}`}>
                      {t.consentGiven ? 'Consent ✓' : 'No consent'}
                    </span>
                  </div>
                  <p className="muted fs-sm" style={{ margin: '0 0 var(--space-2)' }}>
                    {t.relationship} ·{' '}
                    {t.dateOfBirth ? new Date(t.dateOfBirth).toLocaleDateString() : '—'}
                    {t.nationality && ` · ${t.nationality}`}
                  </p>
                  <div className="row tight" style={{ marginBottom: 'var(--space-2)' }}>
                    {t.documentNumberMasked && (
                      <span className="tag outline">📄 {t.documentNumberMasked}</span>
                    )}
                    {t.phoneMasked && <span className="tag outline">📱 {t.phoneMasked}</span>}
                  </div>
                  <button
                    className="btn"
                    onClick={() => void deleteTraveler(t.id)}
                    disabled={busyTraveler}
                    style={{ width: '100%' }}
                  >
                    🗑 Delete
                  </button>
                </article>
              );
            })}
          </div>
        )}
      </div>

      <SectionTitle
        title="👨‍👩‍👧 Family groups"
        subtitle="Group tickets and shared travel for you and the people you travel with."
      />
      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        {familyMessage && <Alert kind="success">{familyMessage}</Alert>}
        {familyError && <Alert kind="danger">{familyError}</Alert>}
        {groupsError && <Alert kind="danger">{groupsError}</Alert>}

        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr', marginBottom: 'var(--space-3)' }}>
          <form onSubmit={(e) => void createGroup(e)}>
            <div className="field">
              <label htmlFor="newGroupName">Create a new group</label>
              <input
                id="newGroupName"
                required
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                placeholder="e.g. The Smiths"
              />
            </div>
            <button className="btn primary block" type="submit" disabled={busyFamily}>
              {busyFamily ? 'Creating…' : '➕ Create group'}
            </button>
          </form>
          <form onSubmit={(e) => void joinGroup(e)}>
            <div className="field">
              <label htmlFor="joinFamilyId">Join an existing group</label>
              <input
                id="joinFamilyId"
                type="number"
                min={1}
                value={joinFamilyId}
                onChange={(e) => setJoinFamilyId(e.target.value)}
                placeholder="Group ID"
              />
            </div>
            <button className="btn block" type="submit" disabled={busyFamily}>
              🤝 Join group
            </button>
          </form>
        </div>

        {membersError && <Alert kind="danger">{membersError}</Alert>}

        {groups.length === 0 ? (
          <EmptyState
            icon="👨‍👩‍👧"
            title="No family groups yet"
            description="Create one above or join an existing group by ID."
          />
        ) : (
          <div className="stack">
            {groups.map((g) => {
              const isExpanded = expandedGroup === g.id;
              return (
                <article className="card compact" key={g.id}>
                  <div className="row between center">
                    <div>
                      <strong>👥 {g.name}</strong>
                      <p
                        className="muted fs-sm"
                        style={{ margin: '0.2rem 0 0' }}
                      >
                        {g.memberCount} member{g.memberCount === 1 ? '' : 's'} · ID #{g.id} ·{' '}
                        Created {new Date(g.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <button className="btn" onClick={() => void toggleMembers(g)}>
                      {isExpanded ? '▲ Hide' : '▼ Members'}
                    </button>
                  </div>
                  {isExpanded && (
                    <div style={{ marginTop: 'var(--space-3)' }}>
                      {loadingMembersId === g.id && <Skeleton lines={2} />}
                      {loadingMembersId !== g.id && membersByGroup[g.id] && (
                        <>
                          {membersByGroup[g.id]!.length === 0 ? (
                            <p className="muted fs-sm" style={{ margin: 0 }}>
                              No members yet.
                            </p>
                          ) : (
                            <div className="stack tight">
                              {membersByGroup[g.id]!.map((m) => (
                                <div
                                  key={m.userId}
                                  className="row between center"
                                  style={{
                                    padding: 'var(--space-2)',
                                    background: 'var(--surface-2)',
                                    borderRadius: 'var(--radius-sm)',
                                  }}
                                >
                                  <span>
                                    👤 {m.username}{' '}
                                    <span className="muted fs-sm">#{m.userId}</span>
                                  </span>
                                  <span className="row tight">
                                    <span className="tag outline">{m.role}</span>
                                    <span className="muted fs-xs">
                                      {new Date(m.joinedAt).toLocaleDateString()}
                                    </span>
                                  </span>
                                </div>
                              ))}
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </div>

      <SectionTitle
        title="🎁 Referrals & invite friends"
        subtitle="Share your code — every invited friend who joins earns you reward points."
      />
      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        {referralMessage && <Alert kind="success">{referralMessage}</Alert>}
        {referralError && <Alert kind="danger">{referralError}</Alert>}

        <div
          className="card compact"
          style={{
            background: 'var(--gradient-soft)',
            borderColor: 'transparent',
            marginBottom: 'var(--space-3)',
          }}
        >
          <div className="row between center" style={{ flexWrap: 'wrap', gap: 'var(--space-3)' }}>
            <div>
              <span className="eyebrow">Your referral code</span>
              {myCode ? (
                <div
                  style={{
                    fontSize: '1.5rem',
                    fontWeight: 700,
                    fontFamily: 'monospace',
                    marginTop: 4,
                    color: 'var(--primary-700)',
                  }}
                >
                  {myCode}
                </div>
              ) : (
                <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>
                  You don't have a code yet.
                </p>
              )}
            </div>
            {myCode ? (
              <button className="btn primary" onClick={() => void copyCode()}>
                📋 Copy code
              </button>
            ) : (
              <button
                className="btn primary"
                onClick={() => void createMyCode()}
                disabled={busyReferral}
              >
                {busyReferral ? 'Creating…' : '✨ Create my code'}
              </button>
            )}
          </div>
        </div>

        <form className="form" onSubmit={(e) => void inviteFriend(e)}>
          <div className="field">
            <label htmlFor="inviteEmail">Invite a friend by email</label>
            <input
              id="inviteEmail"
              type="email"
              required
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="friend@example.com"
            />
          </div>
          <button
            className="btn primary"
            type="submit"
            disabled={busyReferral || !myCode}
            title={!myCode ? 'Create your code first' : undefined}
          >
            {busyReferral ? (
              <>
                <span className="spinner" />
                Sending…
              </>
            ) : (
              '📨 Send invite'
            )}
          </button>
        </form>

        {referralsError && <Alert kind="danger">{referralsError}</Alert>}

        {referrals.length > 0 && (
          <div style={{ marginTop: 'var(--space-4)' }}>
            <h3 style={{ margin: '0 0 var(--space-2)' }}>📋 Your referrals</h3>
            <div className="stack tight">
              {referrals.map((r) => (
                <div
                  key={r.id}
                  className="card compact"
                  style={{ padding: 'var(--space-2) var(--space-3)' }}
                >
                  <div className="row between center" style={{ flexWrap: 'wrap' }}>
                    <div className="row tight">
                      <code className="tag">{r.code}</code>
                      <span className={`badge ${statusVariant(r.status)}`}>{r.status}</span>
                      <span className="tag outline">⭐ {r.rewardPoints} pts</span>
                    </div>
                    <span className="muted fs-sm">
                      {r.inviteeEmailMasked ?? '—'} · Invited{' '}
                      {new Date(r.createdAt).toLocaleDateString()}
                      {r.joinedAt &&
                        ` · Joined ${new Date(r.joinedAt).toLocaleDateString()}`}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <SectionTitle
        title="🛡 Security & privacy"
        subtitle="How we protect your sensitive personal information."
      />
      <div className="card">
        {piiError && <Alert kind="danger">{piiError}</Alert>}
        {pii ? (
          <>
            <div
              className="card compact"
              style={{
                background: pii.piiProtected ? 'var(--gradient-soft)' : 'var(--surface-2)',
                borderColor: 'transparent',
                marginBottom: 'var(--space-3)',
              }}
            >
              <div className="row between center">
                <div>
                  <span className="eyebrow">PII encryption</span>
                  <div
                    style={{
                      fontSize: '1.1rem',
                      fontWeight: 600,
                      marginTop: 4,
                      color: pii.piiProtected ? 'var(--success)' : 'var(--danger)',
                    }}
                  >
                    {pii.piiProtected ? '🟢 Active — at-rest encryption enabled' : '🔴 Inactive'}
                  </div>
                </div>
                <span className={`badge ${pii.piiProtected ? 'success' : 'danger'}`}>
                  {pii.piiProtected ? 'Protected' : 'Unprotected'}
                </span>
              </div>
            </div>
            <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
              <div>
                <span className="eyebrow">Email</span>
                <div style={{ marginTop: 4, fontFamily: 'monospace' }}>{pii.emailMasked}</div>
              </div>
              <div>
                <span className="eyebrow">Phone</span>
                <div style={{ marginTop: 4, fontFamily: 'monospace' }}>{pii.phoneMasked}</div>
              </div>
              <div>
                <span className="eyebrow">Identity document</span>
                <div style={{ marginTop: 4, fontFamily: 'monospace' }}>
                  {pii.identityDocMasked}
                </div>
              </div>
            </div>
            <p className="muted fs-sm" style={{ marginTop: 'var(--space-3)' }}>
              Two-factor authentication and identity verification are available via
              the account security API. Reach out to support to enable them.
            </p>
          </>
        ) : (
          <Skeleton lines={4} />
        )}
      </div>
    </section>
  );
}
