import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  Lock, 
  UserCheck, 
  FileCheck, 
  XCircle,
  Info
} from 'lucide-react';
import { EngineAPI } from '../services/api';

export default function ComplianceView() {
  const [taxonomy, setTaxonomy] = useState([]);
  const [phoneTest, setPhoneTest] = useState('+91 98765 43210');
  const [categoryTest, setCategoryTest] = useState('PROMOTIONAL');
  const [dndResult, setDndResult] = useState(null);

  const [gdprUser, setGdprUser] = useState('usr_robert_51');
  const [consentType, setConsentType] = useState('MARKETING_COMMUNICATIONS');
  const [consentResult, setConsentResult] = useState('');

  useEffect(() => {
    EngineAPI.getTaxonomy().then(setTaxonomy);
    handleCheckDnd();
  }, []);

  const handleCheckDnd = () => {
    const res = EngineAPI.checkDndStatus(phoneTest, categoryTest);
    setDndResult(res);
  };

  const handleRevokeGdpr = () => {
    if (window.confirm(`Revoke all GDPR consents for user ${gdprUser}?`)) {
      setConsentResult(`Successfully revoked all active consent records for user ${gdprUser} under GDPR Right-to-Object (Article 21).`);
      setTimeout(() => setConsentResult(''), 5000);
    }
  };

  return (
    <div className="page-view" id="compliance-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>TRAI &amp; Compliance Policy Hub</h2>
          <p>Regulatory rule evaluation, TRAI National DND clearance simulation, and GDPR consent registry</p>
        </div>
      </div>

      <div className="ops-banner info" style={{ marginBottom: '18px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Info size={15} />
          <span>
            <strong>Policy Evaluator:</strong> Simulates enforcement of TRAI Telecom Commercial Communications Customer Preference Regulations (TCCCPR) and GDPR right to erasure.
          </span>
        </div>
      </div>

      {/* Taxonomy Specification Table */}
      <div className="card" style={{ marginBottom: '20px' }}>
        <div className="card-header">
          <div>
            <div className="card-title">Financial Event Taxonomy &amp; Compliance Rules</div>
            <div className="card-subtitle">Enforcement matrix applied across all inbound domain events</div>
          </div>
        </div>

        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Event Code</th>
                <th>Category</th>
                <th>TRAI DND Policy</th>
                <th>Quiet Hours (22:00-08:00)</th>
                <th>Frequency Cap</th>
                <th>Scope &amp; Description</th>
              </tr>
            </thead>
            <tbody>
              {taxonomy.map(item => (
                <tr key={item.eventCode}>
                  <td>
                    <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>{item.eventCode}</span>
                  </td>
                  <td>
                    <span className="channel-pill">{item.category}</span>
                  </td>
                  <td>
                    <span className={`status-tag ${item.bypassDnd ? 'DELIVERED' : 'DND'}`}>
                      {item.bypassDnd ? 'BYPASSES DND' : 'BLOCKED ON DND'}
                    </span>
                  </td>
                  <td>
                    <span className={`status-tag ${item.respectQuietHours ? 'QUIET' : 'DELIVERED'}`}>
                      {item.respectQuietHours ? 'ENQUEUED' : '24/7 IMMEDIATE'}
                    </span>
                  </td>
                  <td>
                    <span className={`status-tag ${item.respectFreqCap ? 'QUIET' : 'DELIVERED'}`}>
                      {item.respectFreqCap ? 'VELOCITY CAPPED' : 'UNLIMITED'}
                    </span>
                  </td>
                  <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                    {item.description}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid-2">
        {/* Interactive TRAI DND Simulator */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">TRAI DND Clearance Simulator</div>
              <div className="card-subtitle">Validate MSISDN against simulated National Registry</div>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number / MSISDN</label>
            <input 
              type="text" 
              className="form-input" 
              value={phoneTest} 
              onChange={(e) => setPhoneTest(e.target.value)} 
              placeholder="+91 98765 43210 (DND flagged) or other"
            />
            <span className="form-hint">
              Numbers containing '98765' or 'dnd' simulate users on the National DND Registry.
            </span>
          </div>

          <div className="form-group">
            <label className="form-label">Traffic Classification</label>
            <select 
              className="form-select" 
              value={categoryTest} 
              onChange={(e) => setCategoryTest(e.target.value)}
            >
              <option value="PROMOTIONAL">PROMOTIONAL (Offers, Discounts, Pre-approved)</option>
              <option value="TRANSACTIONAL">TRANSACTIONAL (OTP, Settlement, Invoices)</option>
              <option value="CRITICAL">CRITICAL (Fraud Alerts, Suspicious Login)</option>
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '14px' }}>
            <button 
              className="btn btn-primary btn-sm"
              onClick={handleCheckDnd}
              id="btn-test-dnd"
            >
              Evaluate Regulatory Policy
            </button>
          </div>

          {dndResult && (
            <div style={{
              padding: '12px 14px',
              borderRadius: 'var(--radius-sm)',
              background: dndResult.canDeliver ? 'var(--success-bg)' : 'var(--error-bg)',
              border: '1px solid ' + (dndResult.canDeliver ? 'var(--success-border)' : 'var(--error-border)'),
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)' }}>Policy Decision:</span>
                <span className={`status-tag ${dndResult.canDeliver ? 'DELIVERED' : 'DND'}`}>
                  {dndResult.canDeliver ? 'DISPATCH PERMITTED' : 'STRICTLY BLOCKED'}
                </span>
              </div>
              <div style={{ fontSize: '12px', color: dndResult.canDeliver ? 'var(--success)' : 'var(--error)', lineHeight: '1.4' }}>
                {dndResult.reason}
              </div>
            </div>
          )}
        </div>

        {/* GDPR Consent & Right to Erasure */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">GDPR Consent &amp; Erasure Hub</div>
              <div className="card-subtitle">User consent verification and Right to Object</div>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Target User ID</label>
            <input 
              type="text" 
              className="form-input" 
              value={gdprUser} 
              onChange={(e) => setGdprUser(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label className="form-label">Consent Type</label>
            <select 
              className="form-select" 
              value={consentType} 
              onChange={(e) => setConsentType(e.target.value)}
            >
              <option value="MARKETING_COMMUNICATIONS">Marketing &amp; Promotional Messages</option>
              <option value="ANALYTICS_TRACKING">Engagement &amp; CTR Tracking</option>
              <option value="THIRD_PARTY_DELIVERY">Third-Party SMS/Push Gateways</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '8px', marginTop: '14px', justifyContent: 'flex-end' }}>
            <button 
              className="btn btn-secondary btn-sm"
              onClick={() => {
                setConsentResult(`Consent record verified and valid for ${gdprUser} (${consentType}).`);
                setTimeout(() => setConsentResult(''), 4000);
              }}
            >
              Verify Record
            </button>
            <button 
              className="btn btn-danger btn-sm"
              onClick={handleRevokeGdpr}
            >
              Revoke Consent (Art. 21)
            </button>
          </div>

          {consentResult && (
            <div style={{
              marginTop: '14px',
              padding: '10px 12px',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--bg-subtle)',
              border: '1px solid var(--border-default)',
              fontSize: '12px',
              color: 'var(--text-secondary)'
            }}>
              {consentResult}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
