/**
 * Locale utilities — detects user's browser locale at runtime.
 * Used to pre-fill registration and shop-onboarding forms with sensible
 * regional defaults instead of hardcoding values like "LK" or "LKR".
 */

export interface LocaleDefaults {
  countryIso: string;
  currencyIso: string;
  language: string;
  timezone: string;
}

const CURRENCY_BY_REGION: Record<string, string> = {
  US: 'USD', GB: 'GBP', LK: 'INR', IN: 'INR', AU: 'AUD',
  NZ: 'NZD', SG: 'SGD', JP: 'JPY', CA: 'CAD', DE: 'EUR',
  FR: 'EUR', IT: 'EUR', ES: 'EUR', ZA: 'ZAR', KE: 'KES',
  NG: 'NGN', BR: 'BRL', MX: 'MXN', CN: 'CNY', HK: 'HKD',
  TH: 'THB', MY: 'MYR', PH: 'PHP', ID: 'IDR', VN: 'VND',
  PK: 'PKR', BD: 'BDT', NP: 'NPR', MM: 'MMK',
};

const TIMEZONE_BY_REGION: Record<string, string> = {
  US: 'America/New_York', GB: 'Europe/London', LK: 'Asia/Colombo',
  IN: 'Asia/Kolkata', AU: 'Australia/Sydney', SG: 'Asia/Singapore',
  JP: 'Asia/Tokyo', CA: 'America/Toronto', ZA: 'Africa/Johannesburg',
  KE: 'Africa/Nairobi', NG: 'Africa/Lagos', BR: 'America/Sao_Paulo',
  MX: 'America/Mexico_City', CN: 'Asia/Shanghai', HK: 'Asia/Hong_Kong',
  TH: 'Asia/Bangkok', MY: 'Asia/Kuala_Lumpur', PH: 'Asia/Manila',
  ID: 'Asia/Jakarta', VN: 'Asia/Ho_Chi_Minh', PK: 'Asia/Karachi',
  BD: 'Asia/Dhaka', NP: 'Asia/Kathmandu', MM: 'Asia/Yangon',
};

/**
 * Returns sensible defaults from the browser's locale.
 * Falls back to empty-country (user must pick) so no country is assumed.
 */
export function detectLocale(): LocaleDefaults {
  try {
    const lang = navigator.language || 'en';
    const region = lang.split('-')[1]?.toUpperCase() ?? '';
    const langCode = lang.split('-')[0].toLowerCase();

    const currency = region ? (CURRENCY_BY_REGION[region] ?? 'USD') : 'USD';
    const timezone = region
      ? (TIMEZONE_BY_REGION[region] ?? new Intl.DateTimeFormat().resolvedOptions().timeZone)
      : 'UTC';

    return { countryIso: region, currencyIso: currency, language: langCode, timezone };
  } catch {
    return { countryIso: '', currencyIso: 'USD', language: 'en', timezone: 'UTC' };
  }
}

/** List of supported countries for the form dropdown */
export const COUNTRIES = [
  { code: 'LK', label: 'Sri Lanka' },
  { code: 'IN', label: 'India' },
  { code: 'US', label: 'United States' },
  { code: 'GB', label: 'United Kingdom' },
  { code: 'AU', label: 'Australia' },
  { code: 'SG', label: 'Singapore' },
  { code: 'MY', label: 'Malaysia' },
  { code: 'TH', label: 'Thailand' },
  { code: 'ID', label: 'Indonesia' },
  { code: 'PH', label: 'Philippines' },
  { code: 'VN', label: 'Vietnam' },
  { code: 'PK', label: 'Pakistan' },
  { code: 'BD', label: 'Bangladesh' },
  { code: 'NP', label: 'Nepal' },
  { code: 'CA', label: 'Canada' },
  { code: 'DE', label: 'Germany' },
  { code: 'FR', label: 'France' },
  { code: 'JP', label: 'Japan' },
  { code: 'ZA', label: 'South Africa' },
  { code: 'KE', label: 'Kenya' },
  { code: 'NG', label: 'Nigeria' },
  { code: 'BR', label: 'Brazil' },
  { code: 'MX', label: 'Mexico' },
  { code: 'AE', label: 'UAE' },
  { code: 'SA', label: 'Saudi Arabia' },
  { code: 'QA', label: 'Qatar' },
  { code: 'NZ', label: 'New Zealand' },
] as const;

/** Supported currencies */
export const CURRENCIES = [
  { code: 'USD', label: 'USD — US Dollar' },
  { code: 'EUR', label: 'EUR — Euro' },
  { code: 'GBP', label: 'GBP — British Pound' },
  { code: 'LKR', label: 'LKR — Sri Lankan Rupee' },
  { code: 'INR', label: 'INR — Indian Rupee' },
  { code: 'AUD', label: 'AUD — Australian Dollar' },
  { code: 'SGD', label: 'SGD — Singapore Dollar' },
  { code: 'JPY', label: 'JPY — Japanese Yen' },
  { code: 'CAD', label: 'CAD — Canadian Dollar' },
  { code: 'ZAR', label: 'ZAR — South African Rand' },
  { code: 'KES', label: 'KES — Kenyan Shilling' },
  { code: 'NGN', label: 'NGN — Nigerian Naira' },
] as const;

/** Supported IANA timezones */
export const TIMEZONES = [
  { value: 'Pacific/Auckland', label: 'Pacific/Auckland (NZST)' },
  { value: 'Australia/Sydney', label: 'Australia/Sydney (AEST)' },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo (JST)' },
  { value: 'Asia/Singapore', label: 'Asia/Singapore (SGT)' },
  { value: 'Asia/Jakarta', label: 'Asia/Jakarta (WIB)' },
  { value: 'Asia/Bangkok', label: 'Asia/Bangkok (ICT)' },
  { value: 'Asia/Ho_Chi_Minh', label: 'Asia/Ho_Chi_Minh (ICT)' },
  { value: 'Asia/Kuala_Lumpur', label: 'Asia/Kuala_Lumpur (MYT)' },
  { value: 'Asia/Manila', label: 'Asia/Manila (PHT)' },
  { value: 'Asia/Shanghai', label: 'Asia/Shanghai (CST)' },
  { value: 'Asia/Hong_Kong', label: 'Asia/Hong_Kong (HKT)' },
  { value: 'Asia/Colombo', label: 'Asia/Colombo (SLST)' },
  { value: 'Asia/Kolkata', label: 'Asia/Kolkata (IST)' },
  { value: 'Asia/Karachi', label: 'Asia/Karachi (PKT)' },
  { value: 'Asia/Dhaka', label: 'Asia/Dhaka (BST)' },
  { value: 'Asia/Kathmandu', label: 'Asia/Kathmandu (NPT)' },
  { value: 'Asia/Riyadh', label: 'Asia/Riyadh (AST)' },
  { value: 'Asia/Dubai', label: 'Asia/Dubai (GST)' },
  { value: 'Asia/Qatar', label: 'Asia/Qatar (AST)' },
  { value: 'Africa/Nairobi', label: 'Africa/Nairobi (EAT)' },
  { value: 'Africa/Johannesburg', label: 'Africa/Johannesburg (SAST)' },
  { value: 'Africa/Lagos', label: 'Africa/Lagos (WAT)' },
  { value: 'Europe/London', label: 'Europe/London (GMT/BST)' },
  { value: 'Europe/Paris', label: 'Europe/Paris (CET)' },
  { value: 'Europe/Berlin', label: 'Europe/Berlin (CET)' },
  { value: 'Europe/Amsterdam', label: 'Europe/Amsterdam (CET)' },
  { value: 'America/New_York', label: 'America/New_York (EST/EDT)' },
  { value: 'America/Chicago', label: 'America/Chicago (CST/CDT)' },
  { value: 'America/Denver', label: 'America/Denver (MST/MDT)' },
  { value: 'America/Los_Angeles', label: 'America/Los_Angeles (PST/PDT)' },
  { value: 'America/Toronto', label: 'America/Toronto (EST/EDT)' },
  { value: 'America/Vancouver', label: 'America/Vancouver (PST/PDT)' },
  { value: 'America/Mexico_City', label: 'America/Mexico_City (CST)' },
  { value: 'America/Sao_Paulo', label: 'America/Sao_Paulo (BRT)' },
  { value: 'UTC', label: 'UTC' },
] as const;
