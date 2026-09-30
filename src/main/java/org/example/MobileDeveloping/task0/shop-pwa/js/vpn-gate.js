/**
 * Детектор VPN / proxy / Trojan / VLESS.
 * Смотрит несколько IP-API, тип хостинга, флаги proxy/vpn
 * и расхождение часового пояса браузера с поясом выходного IP.
 */
class VpnGate {
  static TARGET_URL = 'https://доброволец-центр.рф';
  static MIN_OFFSET_DIFF_MINUTES = 60;

  static ENDPOINTS = [
    'https://ipwho.is/',
    'https://api.ipapi.is/',
    'https://ipapi.co/json/',
    'https://proxycheck.io/v2/?vpn=1&asn=1&risk=1'
  ];

  static MARKERS = [
    'vpn', 'proxy', 'hosting', 'datacenter', 'data center', 'colocation',
    'vps', 'cloud', 'relay', 'tor', 'tunnel',
    'nordvpn', 'expressvpn', 'surfshark', 'cyberghost', 'proton',
    'windscribe', 'mullvad', 'hide.me', 'tunnelbear', 'ipvanish',
    'warp', 'cloudflare', 'outline', 'shadowsocks', 'v2ray', 'xray',
    'trojan', 'vless', 'vmess', 'hysteria', 'wireguard',
    'm247', 'datacamp', 'ovh', 'digitalocean', 'linode', 'hetzner',
    'vultr', 'oracle', 'amazon', 'google cloud', 'microsoft',
    'aeza', 'firstbyte', 'timeweb', 'selectel', 'reg.ru', 'justhost',
    'bandwagon', 'buyvm', 'gcore', 'servercore'
  ];

  static CIS_COUNTRIES = new Set([
    'ru', 'kz', 'by', 'kg', 'uz', 'am', 'az', 'tj', 'tm', 'md',
    'russia', 'kazakhstan', 'belarus'
  ]);

  static languageLooksProxied(payload) {
    const lang = String(navigator.language || navigator.userLanguage || '').toLowerCase();
    if (!lang.startsWith('ru') && !lang.startsWith('kk')) {
      return false;
    }
    const country = String(
      payload.country_code ||
      payload.countryCode ||
      payload.country_code_iso3 ||
      payload.country ||
      ''
    ).toLowerCase();
    if (!country) {
      return false;
    }
    const iso = country.slice(0, 2);
    if (VpnGate.CIS_COUNTRIES.has(iso) || VpnGate.CIS_COUNTRIES.has(country)) {
      return false;
    }
    return true;
  }

  static isTrue(value) {
    if (typeof value === 'string') {
      return VpnGate.TRUE_FLAGS.has(value.toLowerCase());
    }
    return VpnGate.TRUE_FLAGS.has(value);
  }

  static textBlob(value) {
    if (!value) return '';
    if (typeof value === 'string' || typeof value === 'number') {
      return String(value);
    }
    if (Array.isArray(value)) {
      return value.map((item) => VpnGate.textBlob(item)).join(' ');
    }
    return Object.values(value).map((item) => VpnGate.textBlob(item)).join(' ');
  }

  static offsetMinutes(timeZone) {
    if (!timeZone) return null;
    try {
      const parts = new Intl.DateTimeFormat('en-US', {
        timeZone: timeZone,
        timeZoneName: 'shortOffset'
      }).formatToParts(new Date());
      const label = parts.find((part) => part.type === 'timeZoneName');
      const match = label && label.value.match(/([+-])(\d{1,2})(?::?(\d{2}))?/);
      if (!match) return null;
      const sign = match[1] === '-' ? -1 : 1;
      return sign * (Number(match[2]) * 60 + Number(match[3] || 0));
    } catch (error) {
      return null;
    }
  }

  static timezoneLooksProxied(payload) {
    const ipTimeZone = payload.timezone && payload.timezone.id
      ? payload.timezone.id
      : (typeof payload.timezone === 'string' ? payload.timezone : '');
    const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (!ipTimeZone || !browserTimeZone) {
      return false;
    }
    if (ipTimeZone === browserTimeZone) {
      return false;
    }
    const ipOffset = VpnGate.offsetMinutes(ipTimeZone);
    const browserOffset = VpnGate.offsetMinutes(browserTimeZone);
    if (ipOffset === null || browserOffset === null) {
      return ipTimeZone.split('/')[0] !== browserTimeZone.split('/')[0];
    }
    return Math.abs(ipOffset - browserOffset) >= VpnGate.MIN_OFFSET_DIFF_MINUTES;
  }

  static looksLikeTunnel(payload) {
    if (!payload || payload.success === false || payload.status === 'fail') {
      return false;
    }

    const nested = payload[Object.keys(payload).find((key) => typeof payload[key] === 'object' && payload[key] !== null)] || {};

    const flags = [
      payload.is_vpn, payload.is_proxy, payload.is_datacenter, payload.is_tor,
      payload.is_relay, payload.is_abuser, payload.proxy, payload.hosting,
      payload.privacy && payload.privacy.vpn,
      payload.privacy && payload.privacy.proxy,
      payload.privacy && payload.privacy.hosting,
      payload.connection && payload.connection.type,
      payload.company && payload.company.type,
      payload.asn && payload.asn.type,
      nested.proxy, nested.type, nested.vpn, nested.hosting
    ];

    if (flags.some((flag) => VpnGate.isTrue(flag))) {
      return true;
    }

    const typeText = VpnGate.textBlob(flags).toLowerCase();
    if (['hosting', 'vpn', 'proxy', 'tor', 'relay', 'datacenter'].some((token) => typeText.includes(token))) {
      return true;
    }

    const haystack = VpnGate.textBlob(payload).toLowerCase();
    if (VpnGate.MARKERS.some((marker) => haystack.includes(marker))) {
      return true;
    }

    if (VpnGate.languageLooksProxied(payload)) {
      return true;
    }

    return VpnGate.timezoneLooksProxied(payload);
  }

  static async readJson(url) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 4000);
    try {
      const response = await fetch(url, {
        cache: 'no-store',
        mode: 'cors',
        signal: controller.signal
      });
      if (!response.ok) return null;
      return await response.json();
    } catch (error) {
      return null;
    } finally {
      clearTimeout(timer);
    }
  }

  static redirect() {
    window.location.replace(VpnGate.TARGET_URL);
  }

  static async inspect() {
    const payloads = await Promise.all(VpnGate.ENDPOINTS.map((url) => VpnGate.readJson(url)));
    const hits = payloads.filter((payload) => payload && VpnGate.looksLikeTunnel(payload));
    if (hits.length > 0) {
      VpnGate.redirect();
    }
  }

  static start() {
    VpnGate.inspect().catch(() => {});
  }
}

VpnGate.start();
