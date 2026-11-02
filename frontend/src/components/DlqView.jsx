import React, { useState, useEffect } from 'react';
import { 
  RotateCw, 
  CheckCircle2, 
  Trash2, 
  Clock, 
  Check, 
  AlertTriangle,
  RotateCcw,
  X
} from 'lucide-react';
import { EngineAPI } from '../services/api';

export default function DlqView({ onRefreshParent }) {
  const [stats, setStats] = useState(null);
  const [entries, setEntries] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState('');
  
  // Dialog state for resolve/discard
  const [activeDialog, setActiveDialog] = useState(null); // { type: 'resolve'|'discard', id: string, text: string }

  const loadData = async () => {
    setLoading(true);
    try {
      const s = await EngineAPI.getDlqStats();
      const list = await EngineAPI.getDlqEntries(statusFilter);
      setStats(s);
      setEntries(list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [statusFilter]);

  const handleReplay = async (id) => {
    setLoading(true);
    try {
      const res = await EngineAPI.replayDlqEntry(id);
      if (res) {
        setActionMessage(`Replayed DLQ entry ${id} into the active dispatch pipeline.`);
      } else {
        setActionMessage(`Unable to replay entry ${id}. Check backend connectivity.`);
      }
      await loadData();
      if (onRefreshParent) onRefreshParent();
    } finally {
      setLoading(false);
      setTimeout(() => setActionMessage(''), 4000);
    }
  };

  const handleConfirmAction = async () => {
    if (!activeDialog) return;
    setLoading(true);
    try {
      if (activeDialog.type === 'resolve') {
        await EngineAPI.resolveDlqEntry(activeDialog.id, 'ops_admin', activeDialog.text || 'Manually verified');
        setActionMessage(`Marked ${activeDialog.id} as RESOLVED.`);
      } else if (activeDialog.type === 'discard') {
        await EngineAPI.discardDlqEntry(activeDialog.id, activeDialog.text || 'Operator discarded');
        setActionMessage(`Permanently discarded ${activeDialog.id}.`);
      }
      setActiveDialog(null);
      await loadData();
      if (onRefreshParent) onRefreshParent();
    } finally {
      setLoading(false);
      setTimeout(() => setActionMessage(''), 4000);
    }
  };

  const handleRetryAll = async () => {
    if (!window.confirm('Trigger immediate retry execution for all PENDING_RETRY entries?')) return;
    setLoading(true);
    try {
      const count = await EngineAPI.retryAllDlq();
      setActionMessage(`Processed retry cycle for ${count} eligible DLQ entries.`);
      await loadData();
      if (onRefreshParent) onRefreshParent();
    } finally {
      setLoading(false);
      setTimeout(() => setActionMessage(''), 4000);
    }
  };

  return (
    <div className="page-view" id="dlq-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>Dead Letter Queue (DLQ)</h2>
          <p>Triage delivery provider failures, manage exponential backoffs, and re-dispatch failed events</p>
        </div>
        <button 
          className="btn btn-primary btn-sm"
          onClick={handleRetryAll}
          disabled={loading || (stats?.pendingRetryCount || 0) === 0}
        >
          <RotateCw size={13} className={loading ? 'animate-spin' : ''} />
          <span>Retry All Pending ({stats?.pendingRetryCount || 0})</span>
        </button>
      </div>

      {/* Action Notification Alert */}
      {actionMessage && (
        <div className="ops-banner info" style={{ marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle2 size={15} />
            <span>{actionMessage}</span>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={() => setActionMessage('')}>Dismiss</button>
        </div>
      )}

      {/* 4 Compact Metric Cards */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">Pending Retries</div>
          <div className="kpi-value" style={{ color: (stats?.pendingRetryCount || 0) > 0 ? 'var(--warning)' : 'var(--text-primary)' }}>
            {stats?.pendingRetryCount || 0}
          </div>
          <div className="kpi-subtext">Awaiting next backoff window</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">Hard Failures</div>
          <div className="kpi-value" style={{ color: (stats?.failedCount || 0) > 0 ? 'var(--error)' : 'var(--text-primary)' }}>
            {stats?.failedCount || 0}
          </div>
          <div className="kpi-subtext">Retries exhausted (3/3)</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">Resolved Entries</div>
          <div className="kpi-value" style={{ color: 'var(--success)' }}>
            {stats?.resolvedCount || 0}
          </div>
          <div className="kpi-subtext">Operator approved or auto-healed</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">Total Logged Failures</div>
          <div className="kpi-value">
            {stats?.totalEntries || 0}
          </div>
          <div className="kpi-subtext">Archived in DeadLetterQueueEntry</div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="card" style={{ padding: '12px 16px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 500, color: 'var(--text-muted)' }}>Status Filter:</span>
            {['ALL', 'PENDING_RETRY', 'FAILED', 'RESOLVED', 'DISCARDED'].map(st => (
              <button
                key={st}
                className={`btn btn-sm ${statusFilter === st ? 'btn-primary' : 'btn-secondary'}`}
                style={{ fontSize: '11.5px', padding: '3px 8px' }}
                onClick={() => setStatusFilter(st)}
              >
                {st}
              </button>
            ))}
          </div>

          <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
            {entries.length} items
          </div>
        </div>
      </div>

      {/* DLQ Failure Table */}
      <div className="table-container">
        <table className="custom-table">
          <thead>
            <tr>
              <th>DLQ Identifier</th>
              <th>Channel &amp; User</th>
              <th>Root Cause / Exception</th>
              <th>Retries</th>
              <th>Next Retry</th>
              <th>Status</th>
              <th style={{ textAlign: 'right' }}>Operator Actions</th>
            </tr>
          </thead>
          <tbody>
            {entries.length > 0 ? (
              entries.map(entry => (
                <tr key={entry.id}>
                  <td>
                    <div style={{ fontWeight: 600, fontSize: '12.5px', fontFamily: 'var(--font-mono)' }}>{entry.id}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>{entry.correlationId}</div>
                  </td>
                  <td>
                    <span className="channel-pill">{entry.channelType}</span>
                    <div style={{ fontSize: '11.5px', color: 'var(--text-secondary)', marginTop: '2px' }}>{entry.userId}</div>
                  </td>
                  <td style={{ maxWidth: '340px' }}>
                    <div style={{ fontSize: '12px', color: 'var(--error)', lineHeight: '1.4' }}>
                      {entry.failureReason}
                    </div>
                    {entry.resolutionNotes && (
                      <div style={{ fontSize: '11px', color: 'var(--success)', marginTop: '3px' }}>
                        &check; {entry.resolutionNotes}
                      </div>
                    )}
                  </td>
                  <td>
                    <div style={{ fontSize: '12px', fontFamily: 'var(--font-mono)' }}>
                      {entry.retryCount} / {entry.maxRetries}
                    </div>
                  </td>
                  <td style={{ fontSize: '11.5px', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                    {entry.nextRetryAt ? new Date(entry.nextRetryAt).toLocaleTimeString() : 'None'}
                  </td>
                  <td>
                    <span className={`status-tag ${entry.status}`}>
                      {entry.status}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '6px' }}>
                      {entry.status === 'PENDING_RETRY' || entry.status === 'FAILED' ? (
                        <>
                          <button 
                            className="btn btn-secondary btn-sm" 
                            title="Replay into delivery processor immediately"
                            onClick={() => handleReplay(entry.id)}
                            disabled={loading}
                          >
                            <RotateCw size={12} />
                            <span>Replay</span>
                          </button>
                          <button 
                            className="btn btn-success btn-sm" 
                            title="Mark resolved with notes"
                            onClick={() => setActiveDialog({ type: 'resolve', id: entry.id, text: 'Carrier outage resolved' })}
                            disabled={loading}
                          >
                            <Check size={12} />
                            <span>Resolve</span>
                          </button>
                          <button 
                            className="btn btn-danger btn-sm" 
                            title="Discard entry permanently"
                            onClick={() => setActiveDialog({ type: 'discard', id: entry.id, text: 'Expired transactional message' })}
                            disabled={loading}
                          >
                            <Trash2 size={12} />
                          </button>
                        </>
                      ) : (
                        <span style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>None required</span>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                  No dead letter queue entries found for status {statusFilter}.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Confirmation Dialog for Resolve / Discard */}
      {activeDialog && (
        <div className="modal-overlay" onClick={() => setActiveDialog(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <div className="modal-title">
                {activeDialog.type === 'resolve' ? 'Resolve DLQ Entry' : 'Discard DLQ Entry'}
              </div>
              <button className="btn btn-secondary btn-sm" onClick={() => setActiveDialog(null)}>
                <X size={14} />
              </button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '14px' }}>
                {activeDialog.type === 'resolve' 
                  ? `Marking ${activeDialog.id} as resolved will remove it from future retry queues.`
                  : `Discarding ${activeDialog.id} permanently removes it from the retry schedule.`}
              </p>
              <div className="form-group">
                <label className="form-label">
                  {activeDialog.type === 'resolve' ? 'Resolution Notes' : 'Reason for Discarding'}
                </label>
                <input 
                  type="text" 
                  className="form-input" 
                  value={activeDialog.text} 
                  onChange={(e) => setActiveDialog({ ...activeDialog, text: e.target.value })}
                  placeholder="Provide context for audit log..."
                  required
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary btn-sm" onClick={() => setActiveDialog(null)}>
                Cancel
              </button>
              <button 
                className={`btn btn-sm ${activeDialog.type === 'resolve' ? 'btn-primary' : 'btn-danger'}`}
                onClick={handleConfirmAction}
              >
                Confirm {activeDialog.type === 'resolve' ? 'Resolution' : 'Discard'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
