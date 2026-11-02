import React from 'react';
import { 
  LayoutDashboard, 
  Send, 
  ListFilter, 
  AlertTriangle, 
  GitFork, 
  FileCode2, 
  ShieldCheck, 
  BarChart3, 
  Radio
} from 'lucide-react';

export default function Sidebar({ currentTab, onSelectTab, dlqCount, backendLive }) {
  const navSections = [
    {
      title: 'Overview',
      items: [
        { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard }
      ]
    },
    {
      title: 'Event Processing',
      items: [
        { id: 'ingest', label: 'Event Ingestion', icon: Send },
        { id: 'notifications', label: 'Notification Stream', icon: ListFilter },
        { 
          id: 'dlq', 
          label: 'Dead Letter Queue', 
          icon: AlertTriangle, 
          badge: dlqCount > 0 ? dlqCount : null,
          badgeType: 'danger'
        }
      ]
    },
    {
      title: 'Routing & Configuration',
      items: [
        { id: 'router', label: 'Router & Simulator', icon: GitFork },
        { id: 'templates', label: 'Template Studio', icon: FileCode2 },
        { id: 'compliance', label: 'TRAI & Compliance', icon: ShieldCheck }
      ]
    },
    {
      title: 'Insights',
      items: [
        { id: 'analytics', label: 'Analytics', icon: BarChart3 }
      ]
    }
  ];

  return (
    <aside className="sidebar" id="app-sidebar">
      {/* Brand Header */}
      <div className="sidebar-header">
        <div className="brand-icon-box">
          <Radio size={16} strokeWidth={2.2} />
        </div>
        <div className="brand-meta">
          <h1>Notification Engine</h1>
          <span>Operations Console</span>
        </div>
      </div>

      {/* Structured Navigation Groups */}
      <nav className="sidebar-nav">
        {navSections.map(section => (
          <div key={section.title} className="nav-group">
            <div className="nav-group-title">{section.title}</div>
            <div className="nav-group-items">
              {section.items.map(item => {
                const Icon = item.icon;
                const isActive = currentTab === item.id;
                return (
                  <div
                    key={item.id}
                    id={`nav-item-${item.id}`}
                    className={`nav-item ${isActive ? 'active' : ''}`}
                    onClick={() => onSelectTab(item.id)}
                  >
                    <Icon size={16} strokeWidth={isActive ? 2 : 1.7} />
                    <span>{item.label}</span>
                    {item.badge && (
                      <span className={`nav-badge ${item.badgeType || ''}`}>
                        {item.badge}
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* Footer Status */}
      <div className="sidebar-footer">
        <div className="env-status-pill">
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span className={`status-dot ${backendLive ? 'live' : 'mock'}`}></span>
            <span>{backendLive ? 'Production / Live API' : 'Simulation Mode'}</span>
          </div>
          <span style={{ fontSize: '10.5px', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
            v3.3.5
          </span>
        </div>
      </div>
    </aside>
  );
}
