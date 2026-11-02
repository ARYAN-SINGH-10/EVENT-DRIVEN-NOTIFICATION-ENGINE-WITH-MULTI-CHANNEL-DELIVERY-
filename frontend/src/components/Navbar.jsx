import React, { useState } from 'react';
import { 
  RefreshCw, 
  Plus, 
  Server, 
  Radio
} from 'lucide-react';

export default function Navbar({ 
  currentTab, 
  backendLive, 
  onRefresh, 
  onOpenQuickIngest 
}) {
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    await onRefresh();
    setTimeout(() => setRefreshing(false), 400);
  };

  const getSectionMetadata = () => {
    switch (currentTab) {
      case 'dashboard': return { category: 'Overview', title: 'Dashboard' };
      case 'ingest': return { category: 'Event Processing', title: 'Event Ingestion' };
      case 'notifications': return { category: 'Event Processing', title: 'Notification Stream' };
      case 'dlq': return { category: 'Event Processing', title: 'Dead Letter Queue' };
      case 'router': return { category: 'Routing & Configuration', title: 'Router & Simulator' };
      case 'templates': return { category: 'Routing & Configuration', title: 'Template Studio' };
      case 'compliance': return { category: 'Routing & Configuration', title: 'TRAI & Compliance' };
      case 'analytics': return { category: 'Insights', title: 'Analytics' };
      default: return { category: 'Console', title: 'Operations' };
    }
  };

  const meta = getSectionMetadata();

  return (
    <header className="top-navbar" id="top-navbar">
      {/* Breadcrumb Navigation */}
      <div className="navbar-breadcrumbs">
        <span className="breadcrumb-root">{meta.category}</span>
        <span className="breadcrumb-sep">/</span>
        <span className="breadcrumb-current">{meta.title}</span>
      </div>

      {/* Actions & Status */}
      <div className="navbar-actions">
        {/* Environment status */}
        <div 
          className={`badge-env ${backendLive ? 'connected' : 'simulation'}`}
          title={backendLive ? 'Connected to live Spring Boot API on port 8080' : 'Backend is currently offline. Operating in high-fidelity local simulation mode.'}
        >
          <span className={`status-dot ${backendLive ? 'live' : 'mock'}`}></span>
          <span>{backendLive ? 'Spring Boot 8080 (Connected)' : 'Simulation Mode'}</span>
        </div>

        {/* Refresh button */}
        <button 
          className="btn btn-secondary btn-sm" 
          onClick={handleRefresh}
          title="Refresh view data"
          id="btn-refresh-stats"
          disabled={refreshing}
        >
          <RefreshCw size={13} className={refreshing ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </button>

        {/* Ingest event button */}
        <button 
          className="btn btn-primary btn-sm" 
          onClick={onOpenQuickIngest}
          id="btn-quick-ingest-trigger"
        >
          <Plus size={14} />
          <span>Ingest Event</span>
        </button>
      </div>
    </header>
  );
}
