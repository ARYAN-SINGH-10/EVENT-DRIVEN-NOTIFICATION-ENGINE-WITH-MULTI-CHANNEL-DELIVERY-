import React, { useState } from 'react';
import { 
  Send, 
  RotateCcw, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  ShieldCheck, 
  Code,
  Copy,
  Check
} from 'lucide-react';
import { EngineAPI } from '../services/api';

const PRESETS = [
  {
    code: 'TXNX-001',
    name: 'Transactional OTP / Payment',
    priority: 'HIGH',
    channel: 'SMS',
    userId: 'usr_sarah_99',
    recipient: '+1 (555) 234-5678',
    data: {
      otp: '782910',
      amount: '250.00',
      currency: 'USD',
      merchant: 'Stripe Payments'
    },
    policy: 'Bypasses TRAI DND. High priority transactional channel routing.'
  },
  {
    code: 'PROMO-001',
    name: 'Promotional Festive Loan Offer',
    priority: 'LOW',
    channel: 'SMS',
    userId: 'usr_dnd_vip',
    recipient: '+91 98765 43210',
    data: {
      loanAmount: '₹10,00,000',
      interestRate: '8.5% APR',
      expiryDays: '3 days'
    },
    policy: 'Subject to TRAI DND validation & Quiet Hours restriction (22:00 - 08:00).'
  },
  {
    code: 'CRIT-001',
    name: 'Critical Security / Fraud Alert',
    priority: 'CRITICAL',
    channel: 'EMAIL',
    userId: 'usr_robert_51',
    recipient: 'robert.m@fintech.io',
    data: {
      accountId: 'ACC-9921',
      suspiciousAmount: '$2,800.00',
      merchant: 'Offshore Crypto Exchange',
      riskScore: '98/100'
    },
    policy: 'Emergency bypass for DND, Quiet Hours, and Frequency caps.'
  },
  {
    code: 'ACC-003',
    name: 'Account Balance Rollup',
    priority: 'NORMAL',
    channel: 'WHATSAPP',
    userId: 'usr_alex_74',
    recipient: '+1 (555) 998-0012',
    data: {
      accountLast4: '4821',
      currency: 'USD',
      balance: '$14,250.40'
    },
    policy: 'Standard notifications with user quiet-hour enqueuing.'
  }
];

export default function EventIngestionView({ onEventDispatched }) {
  const [selectedPreset, setSelectedPreset] = useState(PRESETS[0]);
  const [eventType, setEventType] = useState(PRESETS[0].code);
  const [priority, setPriority] = useState(PRESETS[0].priority);
  const [channel, setChannel] = useState(PRESETS[0].channel);
  const [userId, setUserId] = useState(PRESETS[0].userId);
  const [recipient, setRecipient] = useState(PRESETS[0].recipient);
  const [correlationId, setCorrelationId] = useState('corr-' + Math.random().toString(36).substring(2, 9));
  const [jsonPayload, setJsonPayload] = useState(JSON.stringify(PRESETS[0].data, null, 2));
  
  const [jsonError, setJsonError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [responseLog, setResponseLog] = useState(null);
  const [copied, setCopied] = useState(false);

  const applyPreset = (preset) => {
    setSelectedPreset(preset);
    setEventType(preset.code);
    setPriority(preset.priority);
    setChannel(preset.channel);
    setUserId(preset.userId);
    setRecipient(preset.recipient);
    setJsonPayload(JSON.stringify(preset.data, null, 2));
    setJsonError(null);
  };

  const handleJsonChange = (e) => {
    const val = e.target.value;
    setJsonPayload(val);
    try {
      JSON.parse(val);
      setJsonError(null);
    } catch (err) {
      setJsonError(err.message);
    }
  };

  const handleReset = () => {
    applyPreset(PRESETS[0]);
    setCorrelationId('corr-' + Math.random().toString(36).substring(2, 9));
    setResponseLog(null);
  };

  const handleIngest = async (e) => {
    e.preventDefault();
    let parsedData = {};
    try {
      parsedData = JSON.parse(jsonPayload);
    } catch {
      setJsonError('Invalid JSON format');
      return;
    }

    setLoading(true);
    const payload = {
      eventType,
      priority,
      channel,
      userId,
      recipient,
      correlationId: correlationId || ('corr-' + Math.random().toString(36).substring(2, 9)),
      data: parsedData,
    };

    try {
      const res = await EngineAPI.ingestEvent(payload);
      setResponseLog(res);
      if (onEventDispatched) onEventDispatched();
    } catch (err) {
      setResponseLog({ error: err.message });
    } finally {
      setLoading(false);
    }
  };

  const copyResponse = () => {
    navigator.clipboard.writeText(JSON.stringify(responseLog, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="page-view" id="event-ingestion-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>Event Ingestion</h2>
          <p>Submit domain events into the notification ingestion pipeline (`/api/v1/events`)</p>
        </div>
      </div>

      {/* Preset Toolbar */}
      <div style={{ marginBottom: '18px' }}>
        <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          Standard Taxonomy Presets
        </div>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {PRESETS.map(p => {
            const isSelected = eventType === p.code;
            return (
              <button
                key={p.code}
                type="button"
                className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => applyPreset(p)}
                style={{ fontSize: '12px' }}
              >
                <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>{p.code}</span>
                <span>— {p.name.split(' ')[0]}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="grid-2">
        {/* Ingestion Form */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Event Ingestion Form</div>
              <div className="card-subtitle">Validated event dispatch payload</div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={handleReset} type="button">
              <RotateCcw size={12} />
              <span>Reset</span>
            </button>
          </div>

          <form onSubmit={handleIngest}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label className="form-label">Event Taxonomy Code</label>
                <input 
                  type="text" 
                  className="form-input" 
                  value={eventType} 
                  onChange={(e) => setEventType(e.target.value)} 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Priority</label>
                <select 
                  className="form-select" 
                  value={priority} 
                  onChange={(e) => setPriority(e.target.value)}
                >
                  <option value="CRITICAL">CRITICAL (Emergency)</option>
                  <option value="HIGH">HIGH (Transactional)</option>
                  <option value="NORMAL">NORMAL</option>
                  <option value="LOW">LOW (Promotional)</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label className="form-label">Channel</label>
                <select 
                  className="form-select" 
                  value={channel} 
                  onChange={(e) => setChannel(e.target.value)}
                >
                  <option value="SMS">SMS</option>
                  <option value="EMAIL">EMAIL</option>
                  <option value="PUSH">PUSH</option>
                  <option value="WHATSAPP">WHATSAPP</option>
                  <option value="SLACK">SLACK</option>
                  <option value="IN_APP">IN_APP</option>
                  <option value="VOICE">VOICE</option>
                  <option value="WEBHOOK">WEBHOOK</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Correlation ID</label>
                <input 
                  type="text" 
                  className="form-input" 
                  value={correlationId} 
                  onChange={(e) => setCorrelationId(e.target.value)}
                  placeholder="corr-123456" 
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label className="form-label">Target User ID</label>
                <input 
                  type="text" 
                  className="form-input" 
                  value={userId} 
                  onChange={(e) => setUserId(e.target.value)} 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Recipient Address / Phone</label>
                <input 
                  type="text" 
                  className="form-input" 
                  value={recipient} 
                  onChange={(e) => setRecipient(e.target.value)} 
                  required 
                />
              </div>
            </div>

            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label className="form-label">Payload Data (JSON)</label>
                {jsonError && (
                  <span style={{ fontSize: '11px', color: 'var(--error)' }}>{jsonError}</span>
                )}
              </div>
              <textarea 
                className="form-textarea" 
                rows="6" 
                value={jsonPayload} 
                onChange={handleJsonChange}
                required
                style={{ borderColor: jsonError ? 'var(--error)' : 'var(--border-default)' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '14px' }}>
              <button 
                type="submit" 
                className="btn btn-primary" 
                disabled={loading || !!jsonError}
                id="btn-submit-event"
              >
                <Send size={13} />
                <span>{loading ? 'Submitting...' : 'Ingest Event'}</span>
              </button>
            </div>
          </form>
        </div>

        {/* Pipeline Decision & API Response Detail */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Processing &amp; Decision Trace</div>
              <div className="card-subtitle">Asynchronous pipeline evaluation results</div>
            </div>
            {responseLog && (
              <button className="btn btn-secondary btn-sm" onClick={copyResponse} type="button">
                {copied ? <Check size={12} color="var(--success)" /> : <Copy size={12} />}
                <span>{copied ? 'Copied' : 'Copy'}</span>
              </button>
            )}
          </div>

          {responseLog ? (
            <div>
              {/* Outcome Banner */}
              <div style={{ 
                padding: '12px 14px', 
                borderRadius: 'var(--radius-sm)', 
                marginBottom: '14px',
                background: responseLog.simulatedResult === 'DELIVERED' ? 'var(--success-bg)' : responseLog.simulatedResult === 'DND' ? 'var(--error-bg)' : 'var(--warning-bg)',
                border: '1px solid ' + (responseLog.simulatedResult === 'DELIVERED' ? 'var(--success-border)' : responseLog.simulatedResult === 'DND' ? 'var(--error-border)' : 'var(--warning-border)'),
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '12.5px', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Ingestion Status: {responseLog.status || 'ACCEPTED'}
                  </span>
                  <span className={`status-tag ${responseLog.simulatedResult || 'ACCEPTED'}`}>
                    {responseLog.simulatedResult || 'ACCEPTED'}
                  </span>
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                  {responseLog.message || responseLog.notification?.statusReason}
                </div>
              </div>

              {/* Step Audit Trace */}
              <div style={{ marginBottom: '14px', border: '1px solid var(--border-default)', borderRadius: 'var(--radius-sm)', padding: '12px', background: 'var(--bg-subtle)' }}>
                <div style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '8px' }}>
                  Pipeline Evaluation Trace
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '12px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '7px', color: 'var(--success)' }}>
                    <CheckCircle2 size={13} />
                    <span>1. Ingestion validated &amp; correlation ID assigned</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '7px', color: responseLog.simulatedResult === 'DND' ? 'var(--error)' : 'var(--success)' }}>
                    {responseLog.simulatedResult === 'DND' ? <AlertCircle size={13} /> : <CheckCircle2 size={13} />}
                    <span>2. TRAI DND Compliance filter: {responseLog.simulatedResult === 'DND' ? 'REJECTED (Registered on DND)' : 'PASSED'}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '7px', color: responseLog.simulatedResult === 'QUIET' ? 'var(--warning)' : 'var(--success)' }}>
                    {responseLog.simulatedResult === 'QUIET' ? <Clock size={13} /> : <CheckCircle2 size={13} />}
                    <span>3. Quiet Hours schedule window: {responseLog.simulatedResult === 'QUIET' ? 'ENQUEUED (Night window)' : 'PASSED'}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '7px', color: responseLog.simulatedResult === 'DLQ' ? 'var(--error)' : 'var(--success)' }}>
                    {responseLog.simulatedResult === 'DLQ' ? <AlertCircle size={13} /> : <CheckCircle2 size={13} />}
                    <span>4. Gateway Delivery: {responseLog.simulatedResult === 'DLQ' ? 'TIMEOUT -> DLQ' : 'SUCCESSFUL DISPATCH'}</span>
                  </div>
                </div>
              </div>

              {/* Raw JSON */}
              <div className="code-block" style={{ maxHeight: '180px', overflowY: 'auto' }}>
                {JSON.stringify(responseLog, null, 2)}
              </div>
            </div>
          ) : (
            <div style={{ padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
              <Code size={32} style={{ margin: '0 auto 8px', color: 'var(--border-strong)' }} />
              <div style={{ fontSize: '13px', fontWeight: 500, color: 'var(--text-secondary)' }}>Awaiting Ingestion Submission</div>
              <div style={{ fontSize: '12px', marginTop: '4px' }}>
                Select a preset or customize the payload to dispatch an event into the pipeline.
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
