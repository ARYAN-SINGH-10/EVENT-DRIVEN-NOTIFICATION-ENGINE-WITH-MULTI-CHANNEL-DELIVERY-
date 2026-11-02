import React from 'react';
import { 
  CheckCircle2, 
  AlertTriangle, 
  Activity, 
  Clock, 
  Server, 
  RotateCw,
  ArrowRight,
  ShieldAlert,
  AlertCircle,
  Eye,
  Info
} from 'lucide-react';

export default function DashboardView({ 
  stats, 
  providers, 
  notifications, 
  onSelectTab,
  onOpenIngest 
}) {
  const providerList = Object.values(providers || {});
  const healthyCount = providerList.filter(p => p.healthy).length;
  
  // Extract recent failures and DLQ items for operational alert triage
  const failedEvents = (notifications || []).filter(
    n => n.status === 'DLQ' || n.status === 'FAILED' || n.status === 'DND'
  );

  return (
    <div className="page-view" id="dashboard-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>Operations Dashboard</h2>
          <p>Real-time delivery throughput, channel reliability, and pipeline status</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '11.5px', color: 'var(--text-muted)', marginRight: '6px' }}>
            Updated: {new Date().toLocaleTimeString()}
          </span>
          <button className="btn btn-secondary btn-sm" onClick={() => onSelectTab('analytics')}>
            Analytics
          </button>
          <button className="btn btn-primary btn-sm" onClick={onOpenIngest}>
            Ingest Event
          </button>
        </div>
      </div>

      {/* Subtle Environment Notice */}
      <div className="ops-banner simulation" style={{ padding: '8px 12px', fontSize: '12px', marginBottom: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}>
          <Info size={14} />
          <span>
            <strong>Simulation Environment:</strong> Operating in Kafka Simulation Mode. Compliance policies and gateway responses are evaluated locally.
          </span>
        </div>
      </div>

      {/* 4 Compact KPIs */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">Events Processed</div>
          <div className="kpi-value">{stats?.totalNotificationsProcessed?.toLocaleString() || '14,892'}</div>
          <div className="kpi-subtext">
            <span className="trend-good">+12.4%</span>
            <span>vs prior 24h window</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">Delivery Success Rate</div>
          <div className="kpi-value">{stats?.overallDeliveryRate || 98.4}%</div>
          <div className="kpi-subtext">
            <span className="trend-good">SLA met (&ge;98.0%)</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">Pending DLQ Backlog</div>
          <div className="kpi-value" style={{ color: (stats?.dlqSize || 0) > 0 ? 'var(--warning)' : 'var(--text-primary)' }}>
            {stats?.dlqSize || 0}
          </div>
          <div className="kpi-subtext">
            <a 
              href="#dlq" 
              onClick={(e) => { e.preventDefault(); onSelectTab('dlq'); }}
              style={{ color: 'var(--primary)', textDecoration: 'none', fontWeight: 500 }}
            >
              Manage backlog &rarr;
            </a>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">Provider Gateway Availability</div>
          <div className="kpi-value">
            {healthyCount} / {providerList.length} Online
          </div>
          <div className="kpi-subtext">
            <span>Status: {healthyCount === providerList.length ? 'All Healthy' : 'Degraded Outage'}</span>
          </div>
        </div>
      </div>

      {/* Aggregate Pipeline Architecture & Stage Distribution */}
      <div className="card" style={{ padding: '14px 18px', marginBottom: '18px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div>
            <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
              Aggregate Pipeline Architecture &amp; Stage Distribution
            </div>
            <div style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>
              Global throughput progression across compliance filters, routing, and delivery endpoints
            </div>
          </div>
          <span style={{ fontSize: '11.5px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
            100% Ingestion Fidelity
          </span>
        </div>

        <div className="lifecycle-stepper">
          <div className="step-node completed">
            <CheckCircle2 size={13} />
            <span>1. INGESTION (100%)</span>
          </div>
          <div className="step-divider"></div>

          <div className="step-node completed">
            <CheckCircle2 size={13} />
            <span>2. ENRICHMENT</span>
          </div>
          <div className="step-divider"></div>

          <div className="step-node active">
            <Activity size={13} />
            <span>3. COMPLIANCE &amp; QUIET (98.7% Clear &bull; 0.9% DND &bull; 0.4% Queued)</span>
          </div>
          <div className="step-divider"></div>

          <div className="step-node completed">
            <CheckCircle2 size={13} />
            <span>4. COST ROUTING</span>
          </div>
          <div className="step-divider"></div>

          <div className="step-node completed">
            <CheckCircle2 size={13} />
            <span>5. DELIVERED (98.4%)</span>
          </div>
          <div className="step-divider"></div>

          <div className="step-node failed">
            <AlertCircle size={13} />
            <span>DLQ BACKLOG (0.2%)</span>
          </div>
        </div>
      </div>

      {/* Balanced 2-Column: Operational Alerts & Recent Failures + Provider Gateways */}
      <div className="grid-2">
        {/* Operational Failures & Triage Alert */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">
                <ShieldAlert size={14} color="var(--error)" />
                <span>Operational Alerts &amp; Failures ({failedEvents.length})</span>
              </div>
              <div className="card-subtitle">Exceptions, timeouts, and DND rejections requiring review</div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => onSelectTab('dlq')}>
              Open DLQ
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {failedEvents.slice(0, 3).map((item) => (
              <div 
                key={item.id}
                style={{
                  padding: '9px 12px',
                  borderRadius: 'var(--radius-sm)',
                  background: item.status === 'DLQ' ? 'var(--error-bg)' : 'var(--warning-bg)',
                  border: '1px solid ' + (item.status === 'DLQ' ? 'var(--error-border)' : 'var(--warning-border)'),
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  gap: '8px'
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span style={{ fontWeight: 600, fontSize: '12px' }}>{item.eventType}</span>
                    <span className="channel-pill">{item.channelType}</span>
                    <span className={`status-tag ${item.status}`}>{item.status}</span>
                  </div>
                  <div style={{ fontSize: '11.5px', color: 'var(--text-secondary)', marginTop: '3px' }}>
                    {item.statusReason || 'Provider timeout on dispatch; retries exhausted.'}
                  </div>
                </div>
                <button 
                  className="btn btn-secondary btn-sm" 
                  style={{ fontSize: '11px', padding: '3px 7px' }}
                  onClick={() => onSelectTab('dlq')}
                >
                  Triage
                </button>
              </div>
            ))}
            {failedEvents.length === 0 && (
              <div style={{ textAlign: 'center', padding: '16px', color: 'var(--text-muted)', fontSize: '12.5px' }}>
                &check; No operational failures or DLQ entries in current window.
              </div>
            )}
          </div>
        </div>

        {/* Channel Provider Health & Unit Costs */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Delivery Provider Gateways</div>
              <div className="card-subtitle">Live health status, latency, and cost per message</div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => onSelectTab('router')}>
              Simulator
            </button>
          </div>

          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Channel</th>
                  <th>Gateway</th>
                  <th>Latency</th>
                  <th className="text-right">Unit Cost</th>
                  <th className="text-right">Health</th>
                </tr>
              </thead>
              <tbody>
                {providerList.map((p) => (
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
                    <td className="text-right font-mono" style={{ fontSize: '11.5px' }}>
                      ${p.costPerUnit?.toFixed(3)}
                    </td>
                    <td className="text-right">
                      <span className={`status-tag ${p.healthy ? 'HEALTHY' : 'FAILED'}`}>
                        {p.healthy ? 'UP' : 'DOWN'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Full-Width Recent Event Stream Table */}
      <div className="card">
        <div className="card-header">
          <div>
            <div className="card-title">Recent Event Stream</div>
            <div className="card-subtitle">Latest inbound financial events and deliverability outcomes</div>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={() => onSelectTab('notifications')}>
            View Stream ({notifications?.length || 0})
          </button>
        </div>

        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Event Taxonomy</th>
                <th>Correlation ID</th>
                <th>Channel</th>
                <th>Target User</th>
                <th>Recipient</th>
                <th>State</th>
                <th className="text-right">Unit Cost</th>
                <th className="text-right">Timestamp</th>
                <th className="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {notifications?.slice(0, 8).map((item) => (
                <tr key={item.id}>
                  <td>
                    <span style={{ fontWeight: 600, fontSize: '12.5px' }}>{item.eventType}</span>
                  </td>
                  <td>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: '11.5px', color: 'var(--text-secondary)' }}>
                      {item.correlationId || item.id?.substring(0, 16)}
                    </span>
                  </td>
                  <td>
                    <span className="channel-pill">{item.channelType}</span>
                  </td>
                  <td style={{ fontSize: '12px' }}>
                    {item.userId}
                  </td>
                  <td style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>
                    {item.recipient}
                  </td>
                  <td>
                    <span className={`status-tag ${item.status}`}>
                      {item.status}
                    </span>
                  </td>
                  <td className="text-right font-mono" style={{ fontSize: '11.5px' }}>
                    ${(item.cost || 0).toFixed(3)}
                  </td>
                  <td className="text-right font-mono" style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>
                    {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </td>
                  <td className="text-right">
                    <button 
                      className="btn btn-secondary btn-sm"
                      onClick={() => onSelectTab('notifications')}
                      style={{ fontSize: '11px', padding: '2px 7px' }}
                    >
                      Inspect
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
