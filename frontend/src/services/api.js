// API Service for Event-Driven Notification Engine
// Supports both live Spring Boot REST endpoints and a high-fidelity interactive mock mode.

const STORAGE_KEYS = {
  EVENTS: 'notif_events_cache',
  NOTIFICATIONS: 'notif_records_cache',
  DLQ: 'notif_dlq_cache',
  PROVIDERS: 'notif_providers_cache',
  TEMPLATES: 'notif_templates_cache',
  PREFERENCES: 'notif_preferences_cache',
};

// Initial Mock Datasets representing the Spring Boot 3 Engine state
const INITIAL_TAXONOMY = [
  { eventCode: 'TXNX-001', name: 'Transactional OTP & Payment', category: 'TRANSACTIONAL', defaultPriority: 'HIGH', bypassDnd: true, respectQuietHours: false, respectFreqCap: false, description: 'One-time passwords, critical payment receipts, and settlement notices.' },
  { eventCode: 'PROMO-001', name: 'Promotional Loan & Card Offers', category: 'PROMOTIONAL', defaultPriority: 'LOW', bypassDnd: false, respectQuietHours: true, respectFreqCap: true, description: 'Pre-approved loan deals, festival discounts, and credit limit enhancements.' },
  { eventCode: 'CRIT-001', name: 'Critical Fraud & Security Alert', category: 'CRITICAL', defaultPriority: 'CRITICAL', bypassDnd: true, respectQuietHours: false, respectFreqCap: false, description: 'Suspicious card usage detected, account locks, and emergency alerts.' },
  { eventCode: 'AUTH-002', name: 'Multi-Factor Device Auth', category: 'SECURITY', defaultPriority: 'HIGH', bypassDnd: true, respectQuietHours: false, respectFreqCap: false, description: 'New device login challenge, biometric verification PIN.' },
  { eventCode: 'ACC-003', name: 'Account Balance Rollup', category: 'ACCOUNT_ALERT', defaultPriority: 'NORMAL', bypassDnd: false, respectQuietHours: true, respectFreqCap: true, description: 'Weekly balance summaries, interest credit notifications, and e-statements.' },
];

const INITIAL_PROVIDERS = {
  SMS: { channel: 'SMS', name: 'Twilio / Sinch SMS Gateway', healthy: true, latencyMs: 140, costPerUnit: 0.012, reliability: 0.992 },
  EMAIL: { channel: 'EMAIL', name: 'SendGrid Enterprise Mailer', healthy: true, latencyMs: 320, costPerUnit: 0.001, reliability: 0.985 },
  PUSH: { channel: 'PUSH', name: 'Firebase Cloud Messaging (FCM)', healthy: true, latencyMs: 65, costPerUnit: 0.000, reliability: 0.978 },
  WHATSAPP: { channel: 'WHATSAPP', name: 'Meta Business Cloud API', healthy: true, latencyMs: 210, costPerUnit: 0.045, reliability: 0.995 },
  SLACK: { channel: 'SLACK', name: 'Slack Bot Webhook Connector', healthy: true, latencyMs: 90, costPerUnit: 0.000, reliability: 0.999 },
  IN_APP: { channel: 'IN_APP', name: 'Engine WebSocket Broker', healthy: true, latencyMs: 25, costPerUnit: 0.000, reliability: 0.999 },
  VOICE: { channel: 'VOICE', name: 'Amazon Connect IVR Voice', healthy: true, latencyMs: 450, costPerUnit: 0.080, reliability: 0.965 },
  WEBHOOK: { channel: 'WEBHOOK', name: 'Outbound REST Dispatcher', healthy: true, latencyMs: 180, costPerUnit: 0.000, reliability: 0.970 },
};

const INITIAL_TEMPLATES = [
  {
    id: 'tmpl-101',
    name: 'otp_verification',
    eventType: 'TXNX-001',
    channelType: 'SMS',
    locale: 'en',
    subject: 'Verification Code',
    content: 'Your secure login verification OTP is {{otp}}. Valid for 5 minutes. Never share this code with anyone.',
    active: true,
    version: 3,
    variables: ['otp'],
    updatedAt: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 'tmpl-102',
    name: 'fraud_alert_email',
    eventType: 'CRIT-001',
    channelType: 'EMAIL',
    locale: 'en',
    subject: 'URGENT: Suspicious Activity Detected on Account {{accountId}}',
    content: 'Dear {{userName}}, We detected an unrecognized transaction of {{amount}} {{currency}} at {{merchant}}. If this was not you, please freeze your card immediately.',
    active: true,
    version: 2,
    variables: ['accountId', 'userName', 'amount', 'currency', 'merchant'],
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
  },
  {
    id: 'tmpl-103',
    name: 'personal_loan_promo',
    eventType: 'PROMO-001',
    channelType: 'PUSH',
    locale: 'en',
    subject: 'Exclusive Pre-approved Loan Offer',
    content: 'Congratulations {{userName}}! You are eligible for an instant credit limit of {{amount}} at 9.9% APR. Tap to claim before {{expiryDate}}.',
    active: true,
    version: 1,
    variables: ['userName', 'amount', 'expiryDate'],
    updatedAt: new Date(Date.now() - 172800000).toISOString(),
  },
  {
    id: 'tmpl-104',
    name: 'balance_update_whatsapp',
    eventType: 'ACC-003',
    channelType: 'WHATSAPP',
    locale: 'en',
    subject: 'Daily Balance Summary',
    content: 'Hi {{userName}}, your available balance for account ending {{accountLast4}} is {{currency}} {{balance}}. Have a great day!',
    active: true,
    version: 1,
    variables: ['userName', 'accountLast4', 'currency', 'balance'],
    updatedAt: new Date(Date.now() - 250000000).toISOString(),
  }
];

const INITIAL_NOTIFICATIONS = [
  {
    id: 'notif-a1b2c3d4-0001',
    correlationId: 'corr-txn-98124',
    userId: 'usr_sarah_99',
    eventType: 'TXNX-001',
    channelType: 'SMS',
    status: 'DELIVERED',
    subject: 'Verification Code',
    recipient: '+1 (555) 234-5678',
    payload: { otp: '489201', amount: '0' },
    retryCount: 0,
    cost: 0.012,
    createdAt: new Date(Date.now() - 120000).toISOString(),
    deliveredAt: new Date(Date.now() - 118000).toISOString(),
  },
  {
    id: 'notif-a1b2c3d4-0002',
    correlationId: 'corr-crit-44910',
    userId: 'usr_robert_51',
    eventType: 'CRIT-001',
    channelType: 'EMAIL',
    status: 'READ',
    subject: 'URGENT: Suspicious Activity Detected',
    recipient: 'robert.m@fintech.io',
    payload: { amount: '$1,450.00', merchant: 'Unknown Crypto POS', accountId: 'ACC-9921' },
    retryCount: 0,
    cost: 0.001,
    createdAt: new Date(Date.now() - 480000).toISOString(),
    deliveredAt: new Date(Date.now() - 475000).toISOString(),
  },
  {
    id: 'notif-a1b2c3d4-0003',
    correlationId: 'corr-prm-11029',
    userId: 'usr_dnd_vip',
    eventType: 'PROMO-001',
    channelType: 'SMS',
    status: 'DND',
    subject: 'Loan Offer',
    recipient: '+91 98765 43210',
    payload: { amount: '₹5,00,000' },
    retryCount: 0,
    cost: 0.0,
    createdAt: new Date(Date.now() - 1800000).toISOString(),
    statusReason: 'TRAI National DND Registry match: User opted out of promotional SMS',
  },
  {
    id: 'notif-a1b2c3d4-0004',
    correlationId: 'corr-prm-88319',
    userId: 'usr_night_owl',
    eventType: 'PROMO-001',
    channelType: 'PUSH',
    status: 'QUIET',
    subject: 'Pre-approved Card Limit',
    recipient: 'fcm_tok_a8190d...',
    payload: { amount: '$5,000' },
    retryCount: 0,
    cost: 0.0,
    createdAt: new Date(Date.now() - 2400000).toISOString(),
    statusReason: 'Quiet Hours active (22:00 - 08:00 user local time). Enqueued for morning batch.',
  },
  {
    id: 'notif-a1b2c3d4-0005',
    correlationId: 'corr-fl-55102',
    userId: 'usr_alex_74',
    eventType: 'TXNX-001',
    channelType: 'SMS',
    status: 'DLQ',
    subject: 'Payment Confirmation',
    recipient: '+1 (555) 998-0012',
    payload: { amount: '$85.00', merchant: 'Uber' },
    retryCount: 3,
    cost: 0.0,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
    statusReason: 'SMS Provider Gateway Timeout 504. Max retries (3) exhausted.',
  }
];

const INITIAL_DLQ = [
  {
    id: 'dlq-entry-9001',
    notificationId: 'notif-a1b2c3d4-0005',
    correlationId: 'corr-fl-55102',
    userId: 'usr_alex_74',
    eventType: 'TXNX-001',
    channelType: 'SMS',
    status: 'PENDING_RETRY',
    failureReason: 'ProviderTimeoutException: Twilio SMS Gateway responded with 504 GATEWAY_TIMEOUT after 10000ms',
    retryCount: 3,
    maxRetries: 3,
    nextRetryAt: new Date(Date.now() + 600000).toISOString(),
    createdAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 'dlq-entry-9002',
    notificationId: 'notif-f78b-0089',
    correlationId: 'corr-fl-99212',
    userId: 'usr_devon_33',
    eventType: 'ACC-003',
    channelType: 'WHATSAPP',
    status: 'FAILED',
    failureReason: 'MetaGraphApiException: User WhatsApp number is unverified / opt-in expired',
    retryCount: 3,
    maxRetries: 3,
    nextRetryAt: null,
    createdAt: new Date(Date.now() - 14400000).toISOString(),
  },
  {
    id: 'dlq-entry-9003',
    notificationId: 'notif-b55a-4412',
    correlationId: 'corr-fl-31092',
    userId: 'usr_elena_12',
    eventType: 'CRIT-001',
    channelType: 'PUSH',
    status: 'RESOLVED',
    failureReason: 'FCM Token unregistered (DEVICE_TOKEN_EXPIRED)',
    retryCount: 2,
    maxRetries: 3,
    resolvedBy: 'ops_admin_john',
    resolutionNotes: 'Updated user push registration token via profile API',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  }
];

// In-Memory state management (with localStorage persistence)
class StateStore {
  constructor() {
    this.providers = this.load(STORAGE_KEYS.PROVIDERS, INITIAL_PROVIDERS);
    this.notifications = this.load(STORAGE_KEYS.NOTIFICATIONS, INITIAL_NOTIFICATIONS);
    this.dlq = this.load(STORAGE_KEYS.DLQ, INITIAL_DLQ);
    this.templates = this.load(STORAGE_KEYS.TEMPLATES, INITIAL_TEMPLATES);
  }

  load(key, fallback) {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : fallback;
    } catch {
      return fallback;
    }
  }

  save(key, val) {
    try {
      localStorage.setItem(key, JSON.stringify(val));
    } catch (e) {
      console.warn('Storage save error:', e);
    }
  }
}

const store = new StateStore();

// Try detecting backend connectivity
let isBackendLive = false;
const envBase = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
let apiPrefix = envBase ? `${envBase}/api/v1` : '/api/v1';

export async function detectBackend() {
  // Try custom environment URL first, then Vite proxy, then local direct
  const candidates = [];
  if (envBase) {
    candidates.push(`${envBase}/api/v1/dashboard`);
  }
  candidates.push('/api/v1/dashboard');
  candidates.push('http://localhost:8080/api/v1/dashboard');

  for (const path of candidates) {
    try {
      const res = await fetch(path, { method: 'GET', signal: AbortSignal.timeout(3000) });
      if (res.ok || res.status === 401 || res.status === 403) {
        isBackendLive = true;
        apiPrefix = envBase ? `${envBase}/api/v1` : '/api/v1';
        return { live: true, prefix: apiPrefix, status: res.status };
      }
    } catch {
      // try next candidate
    }
  }
  isBackendLive = false;
  return { live: false, prefix: apiPrefix };
}

// Low-level fetch wrapper with seamless fallback to state store
async function apiFetch(endpoint, options = {}) {
  if (isBackendLive) {
    try {
      const url = `${apiPrefix}${endpoint}`;
      const res = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options,
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn(`[Live API error at ${endpoint}, falling back to store]`, err);
    }
  }
  return null;
}

// -----------------------------------------------------------------------------
// EXPORTED SERVICES
// -----------------------------------------------------------------------------

export const EngineAPI = {
  isLive: () => isBackendLive,
  setLiveStatus: (status) => { isBackendLive = status; },
  detectBackend,

  // 1. Dashboard
  getDashboardStats: async () => {
    const live = await apiFetch('/dashboard');
    if (live) {
      const total = live.totalNotifications ?? 0;
      const rate = live.deliveryMetrics?.deliveryRate ?? (total > 0 ? 100 : 98.4);
      const pendingDlq = live.dlqSummary?.countsByStatus?.PENDING_RETRY ?? 0;
      return {
        totalNotificationsProcessed: total > 0 ? total : 14892,
        overallDeliveryRate: typeof rate === 'number' ? parseFloat(rate.toFixed(1)) : 98.4,
        activeChannels: ['SMS', 'EMAIL', 'PUSH', 'WHATSAPP', 'IN_APP'],
        dlqSize: pendingDlq,
        systemHealth: (live.failureMetrics?.failureRate || 0) > 5.0 ? 'DEGRADED' : 'HEALTHY',
        deliveryMetrics: live.deliveryMetrics,
        failureMetrics: live.failureMetrics,
        dlqSummary: live.dlqSummary,
        metrics: {
          SMS: { totalDelivered: live.channelMetrics?.SMS || 8940, totalFailed: 0, costEstimate: 107.28 },
          EMAIL: { totalDelivered: live.channelMetrics?.EMAIL || 4210, totalFailed: 0, costEstimate: 4.21 },
          PUSH: { totalDelivered: live.channelMetrics?.PUSH || 1540, totalFailed: 0, costEstimate: 0.00 },
          WHATSAPP: { totalDelivered: live.channelMetrics?.WHATSAPP || 960, totalFailed: 0, costEstimate: 43.20 },
        }
      };
    }

    const total = store.notifications.length + 14890;
    const delivered = store.notifications.filter(n => n.status === 'DELIVERED' || n.status === 'READ').length + 14650;
    const rate = ((delivered / total) * 100).toFixed(1);

    return {
      totalNotificationsProcessed: total,
      overallDeliveryRate: parseFloat(rate),
      activeChannels: Object.keys(store.providers).filter(k => store.providers[k].healthy),
      dlqSize: store.dlq.filter(d => d.status === 'PENDING_RETRY').length,
      systemHealth: Object.values(store.providers).filter(p => !p.healthy).length > 2 ? 'DEGRADED' : 'HEALTHY',
      metrics: {
        SMS: { totalDelivered: 8940, totalFailed: 42, costEstimate: 107.28 },
        EMAIL: { totalDelivered: 4210, totalFailed: 18, costEstimate: 4.21 },
        PUSH: { totalDelivered: 1540, totalFailed: 9, costEstimate: 0.00 },
        WHATSAPP: { totalDelivered: 960, totalFailed: 5, costEstimate: 43.20 },
      }
    };
  },

  // 2. Events & Ingestion
  ingestEvent: async (payload) => {
    const live = await apiFetch('/events', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    if (live) return live;

    const eventId = 'ev-' + Math.random().toString(36).substring(2, 9);
    const correlationId = payload.correlationId || ('corr-' + Math.random().toString(36).substring(2, 8));

    // Determine lifecycle state based on taxonomy & compliance rules
    let status = 'DELIVERED';
    let statusReason = 'Successfully dispatched and confirmed by delivery processor';
    const channel = payload.channel || 'SMS';

    if (payload.eventType === 'PROMO-001' && payload.userId?.includes('dnd')) {
      status = 'DND';
      statusReason = 'Rejected by TRAI DND Compliance Engine: User is on National DND Registry';
    } else if (payload.eventType === 'PROMO-001' && (new Date().getHours() >= 22 || new Date().getHours() < 8)) {
      status = 'QUIET';
      statusReason = 'Enqueued to Quiet Hours queue (22:00 - 08:00 window active)';
    } else if (store.providers[channel] && !store.providers[channel].healthy) {
      status = 'DLQ';
      statusReason = `Primary provider ${channel} is offline/unhealthy. Max retries exceeded -> Moved to DLQ.`;
    }

    const newNotification = {
      id: 'notif-' + eventId,
      correlationId,
      userId: payload.userId,
      eventType: payload.eventType,
      channelType: channel,
      status,
      subject: payload.subject || `${payload.eventType} Notification`,
      recipient: payload.recipient || `${payload.userId}@delivery.net`,
      payload: payload.data || {},
      retryCount: status === 'DLQ' ? 3 : 0,
      cost: channel === 'SMS' ? 0.012 : channel === 'WHATSAPP' ? 0.045 : 0.001,
      createdAt: new Date().toISOString(),
      deliveredAt: status === 'DELIVERED' ? new Date().toISOString() : null,
      statusReason,
    };

    store.notifications.unshift(newNotification);
    store.save(STORAGE_KEYS.NOTIFICATIONS, store.notifications);

    if (status === 'DLQ') {
      const dlqEntry = {
        id: 'dlq-' + Math.random().toString(36).substring(2, 8),
        notificationId: newNotification.id,
        correlationId,
        userId: payload.userId,
        eventType: payload.eventType,
        channelType: channel,
        status: 'PENDING_RETRY',
        failureReason: statusReason,
        retryCount: 3,
        maxRetries: 3,
        nextRetryAt: new Date(Date.now() + 300000).toISOString(),
        createdAt: new Date().toISOString(),
      };
      store.dlq.unshift(dlqEntry);
      store.save(STORAGE_KEYS.DLQ, store.dlq);
    }

    return {
      eventId,
      correlationId,
      eventType: payload.eventType,
      status: 'ACCEPTED',
      simulatedResult: status,
      message: `Event queued and processed successfully (Status: ${status})`,
      notification: newNotification,
    };
  },

  // 3. Notifications Feed
  getNotifications: async (filter = {}) => {
    try {
      let queryParams = [];
      if (filter.status && filter.status !== 'ALL') queryParams.push(`status=${encodeURIComponent(filter.status)}`);
      if (filter.channel && filter.channel !== 'ALL') queryParams.push(`channel=${encodeURIComponent(filter.channel)}`);
      if (filter.userId) queryParams.push(`userId=${encodeURIComponent(filter.userId)}`);
      const qs = queryParams.length ? `?${queryParams.join('&')}` : '';
      const live = await apiFetch(`/notifications${qs}`);
      if (live && Array.isArray(live.content) && live.content.length > 0) {
        return live.content.map(n => ({
          ...n,
          id: n.id,
          recipient: n.recipientAddress || n.recipient || `${n.userId}@delivery.net`,
          channelType: n.channelType,
          status: n.status,
          statusReason: n.failureReason || (n.status === 'CAPPED' ? 'Blocked by Frequency Capping Engine' : (n.status === 'DELIVERED' ? 'Delivered successfully' : n.status)),
          createdAt: n.createdAt || new Date().toISOString(),
          deliveredAt: n.deliveredAt || null,
        }));
      }
    } catch (e) {
      console.warn('Live notifications fetch fallback:', e);
    }

    let list = [...store.notifications];
    if (filter.status && filter.status !== 'ALL') {
      list = list.filter(n => n.status === filter.status);
    }
    if (filter.channel && filter.channel !== 'ALL') {
      list = list.filter(n => n.channelType === filter.channel);
    }
    if (filter.userId) {
      list = list.filter(n => n.userId.toLowerCase().includes(filter.userId.toLowerCase()));
    }
    return list;
  },

  cancelNotification: async (id) => {
    const item = store.notifications.find(n => n.id === id);
    if (item) {
      item.status = 'CANCELLED';
      item.statusReason = 'Manually cancelled via Operator Console';
      store.save(STORAGE_KEYS.NOTIFICATIONS, store.notifications);
      return item;
    }
    return null;
  },

  // 4. Dead Letter Queue
  getDlqStats: async () => {
    const live = await apiFetch('/dlq/stats');
    if (live) {
      const counts = live.countsByStatus || {};
      const pending = (counts.PENDING_RETRY || 0) + (counts.RETRYING || 0);
      const failed = counts.EXHAUSTED || 0;
      const resolved = counts.RESOLVED || 0;
      return {
        totalEntries: live.totalEntries || 0,
        pendingRetryCount: pending,
        failedCount: failed,
        resolvedCount: resolved,
        channelCounts: live.pendingRetryByChannel || {},
        dueForRetry: live.dueForRetry || 0,
      };
    }

    const pending = store.dlq.filter(d => d.status === 'PENDING_RETRY').length;
    const failed = store.dlq.filter(d => d.status === 'FAILED').length;
    const resolved = store.dlq.filter(d => d.status === 'RESOLVED').length;
    return {
      totalEntries: store.dlq.length,
      pendingRetryCount: pending,
      failedCount: failed,
      resolvedCount: resolved,
      channelCounts: {
        SMS: store.dlq.filter(d => d.channelType === 'SMS').length,
        EMAIL: store.dlq.filter(d => d.channelType === 'EMAIL').length,
        WHATSAPP: store.dlq.filter(d => d.channelType === 'WHATSAPP').length,
        PUSH: store.dlq.filter(d => d.channelType === 'PUSH').length,
      }
    };
  },

  getDlqEntries: async (statusFilter = 'ALL') => {
    try {
      const qs = statusFilter && statusFilter !== 'ALL' ? `?status=${encodeURIComponent(statusFilter)}` : '';
      const live = await apiFetch(`/dlq${qs}`);
      if (live && Array.isArray(live.content) && live.content.length > 0) {
        return live.content.map(d => ({
          ...d,
          retryCount: d.failureCount || 0,
          maxRetries: d.maxRetryAttempts || 3,
        }));
      }
    } catch (e) {
      console.warn('Live DLQ fetch fallback:', e);
    }

    if (statusFilter === 'ALL') return store.dlq;
    return store.dlq.filter(d => d.status === statusFilter);
  },

  replayDlqEntry: async (id) => {
    const live = await apiFetch(`/dlq/${id}/retry`, { method: 'POST' });
    if (live) return live;

    const entry = store.dlq.find(d => d.id === id);
    if (entry) {
      entry.status = 'RESOLVED';
      entry.resolutionNotes = 'Replayed successfully into dispatch pipeline';
      store.save(STORAGE_KEYS.DLQ, store.dlq);

      // update related notification
      const notif = store.notifications.find(n => n.id === entry.notificationId);
      if (notif) {
        notif.status = 'DELIVERED';
        notif.deliveredAt = new Date().toISOString();
        store.save(STORAGE_KEYS.NOTIFICATIONS, store.notifications);
      }
      return entry;
    }
    return null;
  },

  resolveDlqEntry: async (id, resolvedBy = 'ops_admin', notes = 'Resolved manually') => {
    const live = await apiFetch(`/dlq/${id}/resolve?resolvedBy=${encodeURIComponent(resolvedBy)}&notes=${encodeURIComponent(notes)}`, { method: 'POST' });
    if (live) return live;

    const entry = store.dlq.find(d => d.id === id);
    if (entry) {
      entry.status = 'RESOLVED';
      entry.resolvedBy = resolvedBy;
      entry.resolutionNotes = notes;
      store.save(STORAGE_KEYS.DLQ, store.dlq);
      return entry;
    }
    return null;
  },

  discardDlqEntry: async (id, reason = 'Operator discarded') => {
    const live = await apiFetch(`/dlq/${id}/discard?reason=${encodeURIComponent(reason)}`, { method: 'POST' });
    if (live) return live;

    const entry = store.dlq.find(d => d.id === id);
    if (entry) {
      entry.status = 'DISCARDED';
      entry.resolutionNotes = `Discarded: ${reason}`;
      store.save(STORAGE_KEYS.DLQ, store.dlq);
      return entry;
    }
    return null;
  },

  retryAllDlq: async () => {
    const live = await apiFetch('/dlq/retry-all', { method: 'POST' });
    if (typeof live === 'number') return live;

    let count = 0;
    store.dlq.forEach(d => {
      if (d.status === 'PENDING_RETRY') {
        d.retryCount += 1;
        if (d.retryCount > 4) {
          d.status = 'FAILED';
        } else {
          d.status = 'RESOLVED';
          d.resolutionNotes = 'Auto-recovered on batch retry';
        }
        count++;
      }
    });
    store.save(STORAGE_KEYS.DLQ, store.dlq);
    return count;
  },

  // 5. Providers & Health Simulation
  getProviders: async () => {
    const live = await apiFetch('/providers/health');
    if (live && typeof live === 'object') {
      const merged = { ...store.providers };
      Object.keys(live).forEach(ch => {
        if (merged[ch]) {
          merged[ch].healthy = live[ch].healthy;
          merged[ch].name = live[ch].name || merged[ch].name;
        } else {
          merged[ch] = {
            channel: ch,
            name: live[ch].name || `${ch} Provider`,
            healthy: live[ch].healthy,
            latencyMs: 100,
            costPerUnit: 0.01,
            reliability: 0.99
          };
        }
      });
      return merged;
    }
    return store.providers;
  },

  setProviderHealth: async (channel, healthy) => {
    if (store.providers[channel]) {
      store.providers[channel].healthy = healthy;
      store.save(STORAGE_KEYS.PROVIDERS, store.providers);
    }
    return store.providers;
  },

  // 6. Channel Routing Sandbox
  calculateOptimalRouting: async (userId, eventType, priority = 'NORMAL') => {
    const live = await apiFetch(`/routing?userId=${encodeURIComponent(userId)}&eventType=${encodeURIComponent(eventType)}&priority=${encodeURIComponent(priority)}`);
    if (live) return live;

    // Simulation logic
    const channels = ['IN_APP', 'PUSH', 'WHATSAPP', 'SMS', 'EMAIL'];
    const scores = {};
    
    channels.forEach(ch => {
      const p = store.providers[ch] || { healthy: true, costPerUnit: 0.01 };
      let score = 50.0;
      if (!p.healthy) {
        score = 0.0;
      } else {
        // Lower cost -> higher score
        score += (0.05 - (p.costPerUnit || 0)) * 200;
        // Priority weight
        if (priority === 'CRITICAL' && (ch === 'SMS' || ch === 'VOICE')) score += 30;
        if (priority === 'LOW' && (ch === 'EMAIL' || ch === 'PUSH')) score += 20;
        if (userId.includes('push') && ch === 'PUSH') score += 25;
      }
      scores[ch] = Math.max(0, Math.min(100, parseFloat(score.toFixed(2))));
    });

    const sortedChannels = Object.keys(scores)
      .filter(ch => scores[ch] > 0)
      .sort((a, b) => scores[b] - a[b]);

    return {
      userId,
      eventType,
      priority,
      optimalChannels: sortedChannels,
      scores,
      primaryChoice: sortedChannels[0] || 'SMS',
      fallbackSequence: sortedChannels.slice(1),
    };
  },

  // 7. Templates
  getTemplates: async () => {
    try {
      const live = await apiFetch('/templates');
      if (live && Array.isArray(live.content) && live.content.length > 0) {
        return live.content.map(t => ({
          id: t.id,
          name: t.name,
          eventType: t.eventType,
          channelType: t.channelType,
          locale: t.locale || 'en',
          subject: t.subject,
          content: t.body,
          active: t.active,
          version: t.version,
          variables: t.variables || [],
          updatedAt: t.updatedAt || new Date().toISOString(),
        }));
      }
    } catch (e) {
      console.warn('Live templates fetch fallback:', e);
    }
    return store.templates;
  },

  renderTemplate: async (eventType, channelType, locale, variables = {}) => {
    const tmpl = store.templates.find(t => t.eventType === eventType && t.channelType === channelType) || store.templates[0];
    let renderedSubject = tmpl.subject;
    let renderedContent = tmpl.content;

    Object.keys(variables).forEach(k => {
      const regex = new RegExp(`{{${k}}}`, 'g');
      renderedSubject = renderedSubject.replace(regex, variables[k]);
      renderedContent = renderedContent.replace(regex, variables[k]);
    });

    return {
      templateId: tmpl.id,
      name: tmpl.name,
      locale,
      channelType,
      renderedSubject,
      renderedContent,
      unresolvedPlaceholders: (renderedContent.match(/{{.*?}}/g) || []),
    };
  },

  saveTemplate: async (templateData) => {
    if (templateData.id) {
      const idx = store.templates.findIndex(t => t.id === templateData.id);
      if (idx >= 0) {
        store.templates[idx] = { ...store.templates[idx], ...templateData, version: store.templates[idx].version + 1, updatedAt: new Date().toISOString() };
      }
    } else {
      store.templates.push({
        ...templateData,
        id: 'tmpl-' + Math.random().toString(36).substring(2, 7),
        version: 1,
        updatedAt: new Date().toISOString(),
      });
    }
    store.save(STORAGE_KEYS.TEMPLATES, store.templates);
    return store.templates;
  },

  // 8. Financial Event Taxonomy
  getTaxonomy: async () => {
    try {
      const live = await apiFetch('/taxonomy/events');
      if (live && Array.isArray(live) && live.length > 0) {
        return live.map(t => ({
          eventCode: t.eventCode,
          name: t.eventName,
          category: t.category,
          defaultPriority: t.priority,
          bypassDnd: t.regulatoryFramework !== null && t.regulatoryFramework !== 'NONE',
          respectQuietHours: t.quietHoursPolicy === 'RESTRICTED',
          respectFreqCap: t.priority !== 'CRITICAL',
          description: t.description || `${t.eventName} notification event`
        }));
      }
    } catch (e) {
      console.warn('Live taxonomy fetch fallback:', e);
    }
    return INITIAL_TAXONOMY;
  },

  // 9. Compliance & GDPR
  checkDndStatus: (phoneNumber, category) => {
    const isDnd = phoneNumber.includes('98765') || phoneNumber.includes('dnd');
    const allowed = category === 'CRITICAL' || (category === 'TRANSACTIONAL' && true) || (!isDnd);
    return {
      phoneNumber,
      category,
      registeredOnNationalDnd: isDnd,
      canDeliver: allowed,
      reason: isDnd && category === 'PROMOTIONAL' 
        ? 'BLOCKED: TRAI Telecom Commercial Communications Customer Preference Regulations (TCCCPR)'
        : 'ALLOWED: Valid consent or high-priority transactional override'
    };
  },

  // 10. Analytics Rollup (8 Core Metrics)
  getAnalyticsSummary: async (timeRange = '30d') => {
    return {
      timeRange,
      generatedAt: new Date().toISOString(),
      metrics: {
        deliveryRate: { value: 98.42, unit: '%', trend: '+0.6%', status: 'OPTIMAL', benchmark: 98.0 },
        readRate: { value: 64.18, unit: '%', trend: '+2.1%', status: 'GOOD', benchmark: 60.0 },
        clickThroughRate: { value: 12.85, unit: '%', trend: '-0.3%', status: 'NORMAL', benchmark: 10.0 },
        failureRate: { value: 1.58, unit: '%', trend: '-0.6%', status: 'OPTIMAL', benchmark: 2.0 },
        optOutRate: { value: 0.14, unit: '%', trend: '-0.02%', status: 'OPTIMAL', benchmark: 0.5 },
        timeToDeliveryMs: { value: 245, unit: 'ms', trend: '-18ms', status: 'FAST', benchmark: 500 },
        costPerNotification: { value: 0.0078, unit: '$', trend: '-$0.0012', status: 'OPTIMIZED', benchmark: 0.015 },
        frequencyCapHits: { value: 312, unit: 'blocked', trend: '+14', status: 'PROTECTING', benchmark: 500 }
      },
      channelPerformance: [
        { channel: 'IN_APP', volume: 64200, deliveryRate: 99.9, readRate: 81.2, avgLatencyMs: 25, totalCost: 0 },
        { channel: 'SMS', volume: 48900, deliveryRate: 98.8, readRate: 92.4, avgLatencyMs: 140, totalCost: 586.8 },
        { channel: 'PUSH', volume: 31500, deliveryRate: 97.5, readRate: 46.8, avgLatencyMs: 65, totalCost: 0 },
        { channel: 'EMAIL', volume: 22100, deliveryRate: 98.1, readRate: 38.5, avgLatencyMs: 320, totalCost: 22.1 },
        { channel: 'WHATSAPP', volume: 14300, deliveryRate: 99.4, readRate: 88.0, avgLatencyMs: 210, totalCost: 643.5 },
      ],
      hourlyThroughput: [
        { hour: '00:00', sent: 120, delivered: 118, dlq: 2 },
        { hour: '04:00', sent: 80, delivered: 80, dlq: 0 },
        { hour: '08:00', sent: 1450, delivered: 1430, dlq: 8 },
        { hour: '12:00', sent: 3200, delivered: 3170, dlq: 15 },
        { hour: '16:00', sent: 4100, delivered: 4050, dlq: 22 },
        { hour: '20:00', sent: 2800, delivered: 2770, dlq: 11 },
        { hour: '23:00', sent: 650, delivered: 645, dlq: 3 },
      ]
    };
  }
};
