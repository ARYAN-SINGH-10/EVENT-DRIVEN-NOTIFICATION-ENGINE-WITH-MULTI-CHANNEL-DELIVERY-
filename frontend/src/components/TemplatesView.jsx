import React, { useState, useEffect } from 'react';
import { 
  FileCode2, 
  Save, 
  Check, 
  Globe, 
  Tag, 
  Eye, 
  Info
} from 'lucide-react';
import { EngineAPI } from '../services/api';

export default function TemplatesView() {
  const [templates, setTemplates] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [locale, setLocale] = useState('en');
  const [testVariables, setTestVariables] = useState({
    otp: '918234',
    userName: 'Aryan Singh',
    amount: '$450.00',
    currency: 'USD',
    merchant: 'Amazon Web Services',
    accountId: 'ACC-8912',
    expiryDate: 'tomorrow at 6:00 PM',
    accountLast4: '3819',
    balance: '$5,920.00'
  });
  const [renderedResult, setRenderedResult] = useState(null);
  const [editSubject, setEditSubject] = useState('');
  const [editContent, setEditContent] = useState('');
  const [savedNotice, setSavedNotice] = useState(false);

  const loadTemplates = async () => {
    const list = await EngineAPI.getTemplates();
    setTemplates(list);
    if (list.length > 0 && !selectedTemplate) {
      selectTemplate(list[0]);
    }
  };

  const selectTemplate = (tmpl) => {
    setSelectedTemplate(tmpl);
    setEditSubject(tmpl.subject);
    setEditContent(tmpl.content);
    renderPreview(tmpl, testVariables, locale);
  };

  const renderPreview = async (tmpl, vars, loc) => {
    if (!tmpl) return;
    const res = await EngineAPI.renderTemplate(tmpl.eventType, tmpl.channelType, loc, vars);
    setRenderedResult(res);
  };

  useEffect(() => {
    loadTemplates();
  }, []);

  const handleVarChange = (key, val) => {
    const updated = { ...testVariables, [key]: val };
    setTestVariables(updated);
    renderPreview(selectedTemplate, updated, locale);
  };

  const handleSaveTemplate = async () => {
    if (!selectedTemplate) return;
    const updated = {
      ...selectedTemplate,
      subject: editSubject,
      content: editContent,
    };
    await EngineAPI.saveTemplate(updated);
    setSavedNotice(true);
    await loadTemplates();
    setTimeout(() => setSavedNotice(false), 2000);
  };

  return (
    <div className="page-view" id="templates-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>Template Studio</h2>
          <p>Channel-specific message templates, multi-locale variations, and dynamic tag interpolation</p>
        </div>
      </div>

      <div className="grid-1-2">
        {/* Template Catalog List */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Template Registry</div>
              <div className="card-subtitle">{templates.length} registered templates</div>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {templates.map(tmpl => {
              const isSelected = selectedTemplate?.id === tmpl.id;
              return (
                <div
                  key={tmpl.id}
                  onClick={() => selectTemplate(tmpl)}
                  style={{
                    padding: '10px 12px',
                    borderRadius: 'var(--radius-sm)',
                    background: isSelected ? 'var(--primary-subtle)' : '#ffffff',
                    border: '1px solid ' + (isSelected ? 'var(--primary-border)' : 'var(--border-default)'),
                    cursor: 'pointer',
                    transition: 'all 0.12s ease',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '3px' }}>
                    <span style={{ fontSize: '13px', fontWeight: 600, color: isSelected ? 'var(--primary)' : 'var(--text-primary)' }}>
                      {tmpl.name}
                    </span>
                    <span className="channel-pill">{tmpl.channelType}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '11.5px', color: 'var(--text-muted)' }}>
                    <span>{tmpl.eventType}</span>
                    <span style={{ fontFamily: 'var(--font-mono)' }}>v{tmpl.version}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Template Editor & Preview */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">
                {selectedTemplate?.name}
                <span className="channel-pill" style={{ marginLeft: '8px' }}>v{selectedTemplate?.version}</span>
              </div>
              <div className="card-subtitle">{selectedTemplate?.eventType} &bull; {selectedTemplate?.channelType}</div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                <Globe size={13} color="var(--text-muted)" />
                <select 
                  className="form-select" 
                  value={locale} 
                  onChange={(e) => {
                    setLocale(e.target.value);
                    renderPreview(selectedTemplate, testVariables, e.target.value);
                  }}
                  style={{ padding: '3px 8px', fontSize: '11.5px' }}
                >
                  <option value="en">English (en)</option>
                  <option value="es">Español (es)</option>
                  <option value="hi">Hindi (hi)</option>
                  <option value="fr">Français (fr)</option>
                </select>
              </div>

              <button 
                className="btn btn-primary btn-sm"
                onClick={handleSaveTemplate}
                id="btn-save-template"
              >
                <Save size={12} />
                <span>{savedNotice ? 'Saved' : 'Save Version'}</span>
              </button>
            </div>
          </div>

          {/* Form */}
          <div style={{ marginBottom: '14px' }}>
            <div className="form-group">
              <label className="form-label">Subject Line</label>
              <input 
                type="text" 
                className="form-input" 
                value={editSubject} 
                onChange={(e) => {
                  setEditSubject(e.target.value);
                  renderPreview({ ...selectedTemplate, subject: e.target.value, content: editContent }, testVariables, locale);
                }} 
              />
            </div>

            <div className="form-group">
              <label className="form-label">Message Body Template (Variables: {'{{tag}}'})</label>
              <textarea 
                className="form-textarea" 
                rows="4" 
                value={editContent} 
                onChange={(e) => {
                  setEditContent(e.target.value);
                  renderPreview({ ...selectedTemplate, subject: editSubject, content: e.target.value }, testVariables, locale);
                }} 
              />
            </div>
          </div>

          {/* Test Variables */}
          <div style={{ marginBottom: '14px', padding: '12px', background: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-default)' }}>
            <div style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Test Variable Values
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '8px' }}>
              {selectedTemplate?.variables?.map(v => (
                <div key={v} style={{ background: '#ffffff', padding: '6px 8px', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border-default)' }}>
                  <div style={{ fontSize: '10.5px', fontFamily: 'var(--font-mono)', color: 'var(--primary)', fontWeight: 600 }}>
                    {'{{' + v + '}}'}
                  </div>
                  <input 
                    type="text" 
                    value={testVariables[v] || ''} 
                    onChange={(e) => handleVarChange(v, e.target.value)}
                    style={{
                      width: '100%',
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--text-primary)',
                      fontSize: '12px',
                      outline: 'none',
                      marginTop: '2px',
                      padding: 0
                    }}
                  />
                </div>
              ))}
            </div>
          </div>

          {/* Rendered Preview Box */}
          <div style={{ border: '1px solid var(--border-default)', borderRadius: 'var(--radius-sm)', background: '#ffffff', overflow: 'hidden' }}>
            <div style={{ padding: '8px 12px', background: 'var(--bg-subtle)', borderBottom: '1px solid var(--border-default)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Rendered Preview ({selectedTemplate?.channelType})
              </span>
              <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                {renderedResult?.unresolvedPlaceholders?.length === 0 ? 'All variables populated' : `${renderedResult?.unresolvedPlaceholders?.length} tags pending`}
              </span>
            </div>
            <div style={{ padding: '14px' }}>
              {selectedTemplate?.channelType === 'EMAIL' && (
                <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '6px' }}>
                  Subject: {renderedResult?.renderedSubject || editSubject}
                </div>
              )}
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>
                {renderedResult?.renderedContent || editContent}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
