import React, { useState, useEffect } from 'react';
import { 
  GitFork, 
  ArrowRight, 
  RotateCcw, 
  Info,
  CheckCircle2,
  AlertTriangle,
  Play
} from 'lucide-react';
import { EngineAPI } from '../services/api';

export default function RouterSandboxView({ onRefreshParent }) {
  const [providers, setProviders] = useState({});
  const [userId, setUserId] = useState('usr_sarah_99');
  const [eventType, setEventType] = useState('TXNX-001');
  const [priority, setPriority] = useState('HIGH');
  const [routingResult, setRoutingResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadProviders = async () => {
    const list = await EngineAPI.getProviders();
    setProviders(list);
  };

  const handleCalculate = async () => {
    setLoading(true);
    try {
      const res = await EngineAPI.calculateOptimalRouting(userId, eventType, priority);
      setRoutingResult(res);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProviders();
    handleCalculate();
  }, []);

  const toggleProviderHealth = async (channel, currentHealthy) => {
    const updated = await EngineAPI.setProviderHealth(channel, !currentHealthy);
    setProviders({ ...updated });
    if (onRefreshParent) onRefreshParent();
    // Auto-recalculate
    const res = await EngineAPI.calculateOptimalRouting(userId, eventType, priority);
    setRoutingResult(res);
  };

  const handleResetAllProviders = async () => {
    for (const ch of Object.keys(providers)) {
      await EngineAPI.setProviderHealth(ch, true);
    }
    const fresh = await EngineAPI.getProviders();
    setProviders({ ...fresh });
    if (onRefreshParent) onRefreshParent();
    const res = await EngineAPI.calculateOptimalRouting(userId, eventType, priority);
    setRoutingResult(res);
  };

  const providerList = Object.values(providers);

  return (
    <div className="page-view" id="router-sandbox-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>Router &amp; Gateway Simulator</h2>
          <p>Inspect intelligent cost-optimized channel selection logic and evaluate failover behavior</p>
        </div>
      </div>

      <div className="grid-2">
        {/* Routing Evaluation Configuration */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Routing Decision Evaluator</div>
              <div className="card-subtitle">Compute optimal channel for given parameters</div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '14px' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">User Identifier</label>
              <input 
                type="text" 
                className="form-input" 
                value={userId} 
                onChange={(e) => setUserId(e.target.value)} 
              />
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Event Taxonomy</label>
              <select 
                className="form-select" 
                value={eventType} 
                onChange={(e) => setEventType(e.target.value)}
              >
                <option value="TXNX-001">TXNX-001 (Transactional)</option>
                <option value="PROMO-001">PROMO-001 (Promotional)</option>
                <option value="CRIT-001">CRIT-001 (Critical Fraud)</option>
                <option value="ACC-003">ACC-003 (Account Alert)</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Priority Tier</label>
            <select 
              className="form-select" 
              value={priority} 
              onChange={(e) => setPriority(e.target.value)}
            >
              <option value="CRITICAL">CRITICAL (Emergency delivery override)</option>
              <option value="HIGH">HIGH (Transactional priority)</option>
              <option value="NORMAL">NORMAL</option>
              <option value="LOW">LOW (Cost-optimized batching)</option>
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
            <button 
              className="btn btn-primary btn-sm"
              onClick={handleCalculate}
              disabled={loading}
              id="btn-calculate-routing"
            >
              <Play size={12} />
              <span>Evaluate Decision</span>
            </button>
          </div>

          {/* Outcome Section */}
          {routingResult && (
            <div style={{ borderTop: '1px solid var(--border-default)', paddingTop: '14px' }}>
              <div style={{ padding: '12px', borderRadius: 'var(--radius-sm)', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', marginBottom: '14px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <span style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--primary)', textTransform: 'uppercase' }}>
                    Primary Channel Selection
                  </span>
                  <span className="status-tag DELIVERED">{routingResult.primaryChoice}</span>
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Deliverability Fallback Sequence:
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '6px', flexWrap: 'wrap' }}>
                  <span className="channel-pill" style={{ background: 'var(--primary)', color: '#ffffff', border: 'none' }}>
                    1. {routingResult.primaryChoice}
                  </span>
                  {routingResult.fallbackSequence?.map((ch, idx) => (
                    <React.Fragment key={ch}>
                      <ArrowRight size={12} color="var(--text-muted)" />
                      <span className="channel-pill">{idx + 2}. {ch}</span>
                    </React.Fragment>
                  ))}
                </div>
              </div>

              {/* Channel Score Breakdown Table */}
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '6px' }}>
                Channel Composite Scores (Cost vs. SLA)
              </div>
              <div className="table-container">
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Channel</th>
                      <th>Composite Score</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(routingResult.scores || {}).map(([ch, score]) => (
                      <tr key={ch}>
                        <td>
                          <span className="channel-pill">{ch}</span>
                        </td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', fontWeight: 600 }}>
                              {score > 0 ? `${score} pts` : '0 pts'}
                            </span>
                            <div style={{ width: '60px', height: '4px', background: 'var(--bg-muted)', borderRadius: '2px' }}>
                              <div style={{ width: `${Math.min(100, score)}%`, height: '100%', background: score > 70 ? 'var(--success)' : score > 0 ? 'var(--primary)' : 'var(--error)', borderRadius: '2px' }} />
                            </div>
                          </div>
                        </td>
                        <td>
                          <span className={`status-tag ${score > 0 ? 'HEALTHY' : 'FAILED'}`}>
                            {score > 0 ? 'ELIGIBLE' : 'OFFLINE'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* Provider Chaos Testing Sandbox */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Provider Outage Simulator</div>
              <div className="card-subtitle">Simulate gateway outages to verify failover routing</div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={handleResetAllProviders} type="button">
              <RotateCcw size={12} />
              <span>Reset All UP</span>
            </button>
          </div>

          <div className="ops-banner simulation" style={{ marginBottom: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}>
              <Info size={14} />
              <span><strong>Testing Sandbox:</strong> Toggling gateway status simulates provider failures. It does not affect external vendors.</span>
            </div>
          </div>

          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Channel</th>
                  <th>Gateway</th>
                  <th>Latency</th>
                  <th>State</th>
                  <th style={{ textAlign: 'right' }}>Simulation Toggle</th>
                </tr>
              </thead>
              <tbody>
                {providerList.map(p => (
                  <tr key={p.channel}>
                    <td>
                      <span className="channel-pill">{p.channel}</span>
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                      {p.name?.split(' ')[0] || p.channel}
                    </td>
                    <td style={{ fontSize: '11.5px', fontFamily: 'var(--font-mono)' }}>
                      {p.latencyMs}ms
                    </td>
                    <td>
                      <span className={`status-tag ${p.healthy ? 'HEALTHY' : 'FAILED'}`}>
                        {p.healthy ? 'HEALTHY' : 'SIMULATED OUTAGE'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button 
                        className={`btn btn-sm ${p.healthy ? 'btn-danger' : 'btn-success'}`}
                        onClick={() => toggleProviderHealth(p.channel, p.healthy)}
                      >
                        {p.healthy ? 'Simulate Down' : 'Restore Up'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
