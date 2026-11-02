import React, { useState } from 'react';
import { 
  Search, 
  Eye, 
  X, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  XCircle,
  FileText
} from 'lucide-react';
import { EngineAPI } from '../services/api';

export default function NotificationsView({ notifications, onRefresh }) {
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [channelFilter, setChannelFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedNotif, setSelectedNotif] = useState(null);

  const statuses = ['ALL', 'DELIVERED', 'READ', 'SENT', 'QUIET', 'DND', 'CAPPED', 'DLQ', 'CANCELLED'];
  const channels = ['ALL', 'SMS', 'EMAIL', 'PUSH', 'WHATSAPP', 'SLACK', 'IN_APP', 'VOICE'];

  const filtered = (notifications || []).filter(item => {
    if (statusFilter !== 'ALL' && item.status !== statusFilter) return false;
    if (channelFilter !== 'ALL' && item.channelType !== channelFilter) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const matchUser = item.userId?.toLowerCase().includes(q);
      const matchCorr = item.correlationId?.toLowerCase().includes(q);
      const matchEv = item.eventType?.toLowerCase().includes(q);
      if (!matchUser && !matchCorr && !matchEv) return false;
    }
    return true;
  });

  const handleCancel = async (id) => {
    if (window.confirm('Cancel this pending/scheduled notification?')) {
      await EngineAPI.cancelNotification(id);
      if (onRefresh) onRefresh();
      if (selectedNotif && selectedNotif.id === id) {
        setSelectedNotif({ ...selectedNotif, status: 'CANCELLED', statusReason: 'Manually cancelled via Operator Console' });
      }
    }
  };

  const renderEventLifecycle = (item) => {
    if (!item) return null;
    const status = item.status;

    if (status === 'DND') {
      return (
        <div className="lifecycle-stepper">
          <div className="step-node completed"><CheckCircle2 size={13} /><span>1. INGESTED</span></div>
          <div className="step-divider"></div>
          <div className="step-node completed"><CheckCircle2 size={13} /><span>2. ENRICHED</span></div>
          <div className="step-divider"></div>
          <div className="step-node blocked"><AlertCircle size={13} /><span>3. TRAI DND (BLOCKED)</span></div>
          <div className="step-divider"></div>
          <div className="step-node" style={{ color: 'var(--text-disabled)' }}><span>4. TERMINATED</span></div>
        </div>
      );
    }

    if (status === 'QUIET') {
      return (
        <div className="lifecycle-stepper">
          <div className="step-node completed"><CheckCircle2 size={13} /><span>1. INGESTED</span></div>
          <div className="step-divider"></div>
          <div className="step-node completed"><CheckCircle2 size={13} /><span>2. ENRICHED</span></div>
          <div className="step-divider"></div>
          <div className="step-node delayed"><Clock size={13} /><span>3. QUIET HOURS (ENQUEUED)</span></div>
          <div className="step-divider"></div>
          <div className="step-node" style={{ color: 'var(--text-muted)' }}><span>4. AWAITING 08:00 WINDOW</span></div>
        </div>
      );
    }

    if (status === 'DLQ' || status === 'FAILED') {
      return (
        <div className="lifecycle-stepper">
          <div className="step-node completed"><CheckCircle2 size={13} /><span>1. INGESTED</span></div>
          <div className="step-divider"></div>
          <div className="step-node completed"><CheckCircle2 size={13} /><span>2. ROUTED</span></div>
          <div className="step-divider"></div>
          <div className="step-node failed"><AlertCircle size={13} /><span>3. GATEWAY RETRIES EXHAUSTED (3/3)</span></div>
          <div className="step-divider"></div>
          <div className="step-node failed"><AlertCircle size={13} /><span>4. DEAD LETTER QUEUE</span></div>
        </div>
      );
    }

    if (status === 'CANCELLED') {
      return (
        <div className="lifecycle-stepper">
          <div className="step-node completed"><CheckCircle2 size={13} /><span>1. INGESTED</span></div>
          <div className="step-divider"></div>
          <div className="step-node blocked"><XCircle size={13} /><span>2. CANCELLED BY OPERATOR</span></div>
        </div>
      );
    }

    return (
      <div className="lifecycle-stepper">
        <div className="step-node completed"><CheckCircle2 size={13} /><span>1. INGESTED</span></div>
        <div className="step-divider"></div>
        <div className="step-node completed"><CheckCircle2 size={13} /><span>2. ENRICHED</span></div>
        <div className="step-divider"></div>
        <div className="step-node completed"><CheckCircle2 size={13} /><span>3. COMPLIANCE PASS</span></div>
        <div className="step-divider"></div>
        <div className="step-node completed"><CheckCircle2 size={13} /><span>4. ROUTED</span></div>
        <div className="step-divider"></div>
        <div className={`step-node ${status === 'DELIVERED' || status === 'READ' ? 'completed' : 'active'}`}>
          <CheckCircle2 size={13} />
          <span>5. {status}</span>
        </div>
      </div>
    );
  };

  return (
    <div className="page-view" id="notifications-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>Notification Stream</h2>
          <p>Audit trail of all routed notifications, compliance outcomes, and lifecycle states</p>
        </div>
        <div style={{ fontSize: '12.5px', color: 'var(--text-muted)' }}>
          Showing {filtered.length} of {notifications?.length || 0} events
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="card" style={{ padding: '12px 16px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          {/* Search box */}
          <div style={{ position: 'relative', width: '280px' }}>
            <Search size={14} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input 
              type="text" 
              className="form-input" 
              placeholder="Search user, ID, correlation..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ width: '100%', paddingLeft: '32px', fontSize: '12.5px', padding: '5px 10px 5px 30px' }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {/* Status dropdown */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 500 }}>Status:</span>
              <select 
                className="form-select" 
                value={statusFilter} 
                onChange={(e) => setStatusFilter(e.target.value)}
                style={{ padding: '4px 8px', fontSize: '12px' }}
              >
                {statuses.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>

            {/* Channel dropdown */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 500 }}>Channel:</span>
              <select 
                className="form-select" 
                value={channelFilter} 
                onChange={(e) => setChannelFilter(e.target.value)}
                style={{ padding: '4px 8px', fontSize: '12px' }}
              >
                {channels.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Main Audit Table */}
      <div className="table-container">
        <table className="custom-table">
          <thead>
            <tr>
              <th>Event Type</th>
              <th>Correlation / Notification ID</th>
              <th>Channel</th>
              <th>Recipient</th>
              <th>State</th>
              <th className="text-right">Unit Cost</th>
              <th className="text-right">Created</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length > 0 ? (
              filtered.map(item => (
                <tr key={item.id} style={{ cursor: 'pointer' }} onClick={() => setSelectedNotif(item)}>
                  <td>
                    <span style={{ fontWeight: 600, fontSize: '12.5px' }}>{item.eventType}</span>
                  </td>
                  <td>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: '11.5px', color: 'var(--text-secondary)' }}>
                      {item.correlationId || item.id}
                    </span>
                  </td>
                  <td>
                    <span className="channel-pill">{item.channelType}</span>
                  </td>
                  <td>
                    <div style={{ fontSize: '12px' }}>{item.userId}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{item.recipient}</div>
                  </td>
                  <td>
                    <span className={`status-tag ${item.status}`}>
                      {item.status}
                    </span>
                  </td>
                  <td className="text-right font-mono" style={{ fontSize: '12px' }}>
                    ${(item.cost || 0).toFixed(3)}
                  </td>
                  <td className="text-right font-mono" style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>
                    {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </td>
                  <td style={{ textAlign: 'right' }} onClick={(e) => e.stopPropagation()}>
                    <div style={{ display: 'inline-flex', gap: '6px' }}>
                      <button 
                        className="btn btn-secondary btn-sm"
                        onClick={() => setSelectedNotif(item)}
                        title="Inspect lifecycle & payload"
                      >
                        <Eye size={12} />
                        <span>Inspect</span>
                      </button>
                      {item.status !== 'CANCELLED' && item.status !== 'DELIVERED' && item.status !== 'READ' && (
                        <button 
                          className="btn btn-danger btn-sm"
                          onClick={() => handleCancel(item.id)}
                          title="Cancel dispatch"
                        >
                          <XCircle size={12} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                  No notifications match the active filter criteria.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Inspect Detail Modal */}
      {selectedNotif && (
        <div className="modal-overlay" onClick={() => setSelectedNotif(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '640px' }}>
            <div className="modal-header">
              <div className="modal-title">
                Notification Detail: {selectedNotif.correlationId || selectedNotif.id}
              </div>
              <button className="btn btn-secondary btn-sm" onClick={() => setSelectedNotif(null)}>
                <X size={14} />
              </button>
            </div>

            <div className="modal-body">
              {/* Lifecycle progression */}
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '6px' }}>
                  State Machine Path: {selectedNotif.status}
                </div>
                {renderEventLifecycle(selectedNotif)}
              </div>

              {/* Attributes table */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '14px' }}>
                <div style={{ padding: '10px 12px', border: '1px solid var(--border-default)', borderRadius: 'var(--radius-sm)', background: 'var(--bg-subtle)' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Event Classification</div>
                  <div style={{ fontSize: '13px', fontWeight: 600 }}>{selectedNotif.eventType}</div>
                </div>
                <div style={{ padding: '10px 12px', border: '1px solid var(--border-default)', borderRadius: 'var(--radius-sm)', background: 'var(--bg-subtle)' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Channel &amp; Unit Cost</div>
                  <div style={{ fontSize: '13px', fontWeight: 600 }}>{selectedNotif.channelType} (${(selectedNotif.cost || 0).toFixed(3)})</div>
                </div>
                <div style={{ padding: '10px 12px', border: '1px solid var(--border-default)', borderRadius: 'var(--radius-sm)', background: 'var(--bg-subtle)' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Recipient User</div>
                  <div style={{ fontSize: '13px', fontWeight: 600 }}>{selectedNotif.userId}</div>
                </div>
                <div style={{ padding: '10px 12px', border: '1px solid var(--border-default)', borderRadius: 'var(--radius-sm)', background: 'var(--bg-subtle)' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Delivery Timestamp</div>
                  <div style={{ fontSize: '13px', fontWeight: 600 }}>{selectedNotif.deliveredAt ? new Date(selectedNotif.deliveredAt).toLocaleString() : 'N/A'}</div>
                </div>
              </div>

              {/* Status Reason / Note */}
              {selectedNotif.statusReason && (
                <div style={{ padding: '10px 12px', borderRadius: 'var(--radius-sm)', background: 'var(--warning-bg)', border: '1px solid var(--warning-border)', marginBottom: '14px' }}>
                  <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--warning)' }}>Status Reason / Compliance Note</div>
                  <div style={{ fontSize: '12px', color: 'var(--text-primary)', marginTop: '2px' }}>{selectedNotif.statusReason}</div>
                </div>
              )}

              {/* Payload Data */}
              <div>
                <div style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Payload Data
                </div>
                <div className="code-block" style={{ maxHeight: '160px', overflowY: 'auto' }}>
                  {JSON.stringify(selectedNotif.payload || {}, null, 2)}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary btn-sm" onClick={() => setSelectedNotif(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
