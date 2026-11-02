import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import DashboardView from './components/DashboardView';
import EventIngestionView from './components/EventIngestionView';
import NotificationsView from './components/NotificationsView';
import DlqView from './components/DlqView';
import RouterSandboxView from './components/RouterSandboxView';
import TemplatesView from './components/TemplatesView';
import ComplianceView from './components/ComplianceView';
import AnalyticsView from './components/AnalyticsView';
import { EngineAPI, detectBackend } from './services/api';
import { X, Send } from 'lucide-react';

export default function App() {
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [backendLive, setBackendLive] = useState(false);
  const [stats, setStats] = useState(null);
  const [providers, setProviders] = useState({});
  const [notifications, setNotifications] = useState([]);
  const [dlqStats, setDlqStats] = useState(null);
  const [quickIngestOpen, setQuickIngestOpen] = useState(false);

  // Quick Ingest Form state
  const [quickEventCode, setQuickEventCode] = useState('TXNX-001');
  const [quickUserId, setQuickUserId] = useState('usr_sarah_99');
  const [quickChannel, setQuickChannel] = useState('SMS');
  const [quickData, setQuickData] = useState('{\n  "amount": "99.00",\n  "currency": "USD",\n  "merchant": "Apple Store"\n}');
  const [quickLoading, setQuickLoading] = useState(false);

  const refreshAllData = async () => {
    try {
      const detectRes = await detectBackend();
      setBackendLive(detectRes.live);

      const [dash, provs, notifs, dlq] = await Promise.all([
        EngineAPI.getDashboardStats(),
        EngineAPI.getProviders(),
        EngineAPI.getNotifications(),
        EngineAPI.getDlqStats(),
      ]);

      setStats(dash);
      setProviders(provs);
      setNotifications(notifs);
      setDlqStats(dlq);
    } catch (e) {
      console.warn('Error refreshing dashboard data:', e);
    }
  };

  useEffect(() => {
    refreshAllData();
    const timer = setInterval(refreshAllData, 15000);
    return () => clearInterval(timer);
  }, []);

  const handleQuickIngestSubmit = async (e) => {
    e.preventDefault();
    setQuickLoading(true);
    let parsed = {};
    try {
      parsed = JSON.parse(quickData);
    } catch {
      alert('Invalid JSON data format');
      setQuickLoading(false);
      return;
    }

    await EngineAPI.ingestEvent({
      eventType: quickEventCode,
      userId: quickUserId,
      channel: quickChannel,
      data: parsed,
    });

    setQuickLoading(false);
    setQuickIngestOpen(false);
    await refreshAllData();
    setCurrentTab('notifications');
  };

  return (
    <div className="app-container" id="engine-app-root">
      {/* Sidebar Navigation */}
      <Sidebar 
        currentTab={currentTab} 
        onSelectTab={setCurrentTab}
        dlqCount={dlqStats?.pendingRetryCount || stats?.dlqSize || 0}
        backendLive={backendLive}
      />

      {/* Main Content Shell */}
      <div className="main-wrapper">
        <Navbar 
          currentTab={currentTab}
          backendLive={backendLive}
          onRefresh={refreshAllData}
          onOpenQuickIngest={() => setQuickIngestOpen(true)}
          stats={stats}
        />

        <main style={{ flex: 1 }}>
          {currentTab === 'dashboard' && (
            <DashboardView 
              stats={stats}
              providers={providers}
              notifications={notifications}
              onSelectTab={setCurrentTab}
              onOpenIngest={() => setCurrentTab('ingest')}
            />
          )}

          {currentTab === 'ingest' && (
            <EventIngestionView 
              onEventDispatched={refreshAllData}
            />
          )}

          {currentTab === 'notifications' && (
            <NotificationsView 
              notifications={notifications}
              onRefresh={refreshAllData}
            />
          )}

          {currentTab === 'dlq' && (
            <DlqView 
              onRefreshParent={refreshAllData}
            />
          )}

          {currentTab === 'router' && (
            <RouterSandboxView 
              onRefreshParent={refreshAllData}
            />
          )}

          {currentTab === 'templates' && (
            <TemplatesView />
          )}

          {currentTab === 'compliance' && (
            <ComplianceView />
          )}

          {currentTab === 'analytics' && (
            <AnalyticsView />
          )}
        </main>
      </div>

      {/* Quick Ingest Modal */}
      {quickIngestOpen && (
        <div className="modal-overlay" onClick={() => setQuickIngestOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '520px' }}>
            <div className="modal-header">
              <div className="modal-title">
                Quick Ingest Event
              </div>
              <button 
                className="btn btn-secondary btn-sm" 
                onClick={() => setQuickIngestOpen(false)}
              >
                <X size={14} />
              </button>
            </div>

            <form onSubmit={handleQuickIngestSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Event Taxonomy</label>
                  <select 
                    className="form-select" 
                    value={quickEventCode} 
                    onChange={(e) => setQuickEventCode(e.target.value)}
                  >
                    <option value="TXNX-001">TXNX-001 (Transactional OTP / Payment)</option>
                    <option value="PROMO-001">PROMO-001 (Promotional Loan Offer)</option>
                    <option value="CRIT-001">CRIT-001 (Critical Security Alert)</option>
                    <option value="AUTH-002">AUTH-002 (Two-Factor Challenge)</option>
                    <option value="ACC-003">ACC-003 (Balance Notification)</option>
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div className="form-group">
                    <label className="form-label">User Target ID</label>
                    <input 
                      type="text" 
                      className="form-input" 
                      value={quickUserId} 
                      onChange={(e) => setQuickUserId(e.target.value)}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Channel</label>
                    <select 
                      className="form-select" 
                      value={quickChannel} 
                      onChange={(e) => setQuickChannel(e.target.value)}
                    >
                      <option value="SMS">SMS</option>
                      <option value="EMAIL">EMAIL</option>
                      <option value="PUSH">PUSH</option>
                      <option value="WHATSAPP">WHATSAPP</option>
                      <option value="IN_APP">IN_APP</option>
                      <option value="SLACK">SLACK</option>
                    </select>
                  </div>
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Payload Data (JSON)</label>
                  <textarea 
                    className="form-textarea" 
                    rows="4" 
                    value={quickData} 
                    onChange={(e) => setQuickData(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button 
                  type="button" 
                  className="btn btn-secondary btn-sm" 
                  onClick={() => setQuickIngestOpen(false)}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary btn-sm" 
                  disabled={quickLoading}
                >
                  <Send size={12} />
                  <span>{quickLoading ? 'Submitting...' : 'Ingest & Dispatch'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
