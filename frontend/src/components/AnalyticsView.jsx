import React, { useState, useEffect } from 'react';
import { 
  BarChart3, 
  Calendar, 
  Info
} from 'lucide-react';
import { EngineAPI } from '../services/api';

export default function AnalyticsView() {
  const [data, setData] = useState(null);
  const [range, setRange] = useState('30d');
  const [loading, setLoading] = useState(false);

  const loadAnalytics = async () => {
    setLoading(true);
    try {
      const res = await EngineAPI.getAnalyticsSummary(range);
      setData(res);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnalytics();
  }, [range]);

  const metrics = data?.metrics || {};

  return (
    <div className="page-view" id="analytics-view">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2>8-Metric Notification Analytics</h2>
          <p>Production rollup metrics computed across all notification channels and providers</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Calendar size={13} color="var(--text-muted)" />
          {['7d', '30d', '90d'].map(r => (
            <button
              key={r}
              className={`btn btn-sm ${range === r ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setRange(r)}
            >
              Last {r}
            </button>
          ))}
        </div>
      </div>

      <div className="ops-banner info" style={{ marginBottom: '18px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Info size={15} />
          <span>
            <strong>Simulated Analytics Pipeline:</strong> Demonstrates production rollup aggregation matching the Spring Boot <code>AnalyticsRollupScheduler</code>.
          </span>
        </div>
      </div>

      {/* 8-Metric Operational KPI Grid */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">1. Delivery Success Rate</div>
          <div className="kpi-value">{metrics.deliveryRate?.value}%</div>
          <div className="kpi-subtext">
            <span className="trend-good">{metrics.deliveryRate?.trend}</span>
            <span>Target: &ge;98.0%</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">2. Read / Open Rate</div>
          <div className="kpi-value">{metrics.readRate?.value}%</div>
          <div className="kpi-subtext">
            <span className="trend-good">{metrics.readRate?.trend}</span>
            <span>In-App &amp; SMS highest</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">3. Click-Through Rate (CTR)</div>
          <div className="kpi-value">{metrics.clickThroughRate?.value}%</div>
          <div className="kpi-subtext">
            <span>Actionable CTAs</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">4. Failure Rate</div>
          <div className="kpi-value" style={{ color: (metrics.failureRate?.value || 0) > 2 ? 'var(--error)' : 'var(--text-primary)' }}>
            {metrics.failureRate?.value}%
          </div>
          <div className="kpi-subtext">
            <span className="trend-good">{metrics.failureRate?.trend}</span>
            <span>Sub-2% SLA target</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">5. Opt-Out Rate</div>
          <div className="kpi-value">{metrics.optOutRate?.value}%</div>
          <div className="kpi-subtext">
            <span>TRAI &amp; user opt-outs</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">6. Time-to-Delivery (p95)</div>
          <div className="kpi-value">{metrics.timeToDeliveryMs?.value} ms</div>
          <div className="kpi-subtext">
            <span className="trend-good">{metrics.timeToDeliveryMs?.trend}</span>
            <span>SLA: &le;500ms</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">7. Blended Cost / Notification</div>
          <div className="kpi-value">${metrics.costPerNotification?.value}</div>
          <div className="kpi-subtext">
            <span>Weighted by channel</span>
          </div>
        </div>

        <div className="kpi-card">
          <div className="kpi-label">8. Velocity Cap Hits</div>
          <div className="kpi-value">{metrics.frequencyCapHits?.value}</div>
          <div className="kpi-subtext">
            <span>Spam velocity suppressed</span>
          </div>
        </div>
      </div>

      {/* Channel Performance Table */}
      <div className="card" style={{ marginBottom: '20px' }}>
        <div className="card-header">
          <div>
            <div className="card-title">Channel Performance &amp; Spend Partition</div>
            <div className="card-subtitle">Volumetric deliverability and total expenditure per channel</div>
          </div>
        </div>

        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Channel Processor</th>
                <th>Volume Processed</th>
                <th>Delivery Rate</th>
                <th>Read Rate</th>
                <th>Latency (avg)</th>
                <th>Aggregate Spend</th>
              </tr>
            </thead>
            <tbody>
              {data?.channelPerformance?.map(cp => (
                <tr key={cp.channel}>
                  <td>
                    <span className="channel-pill">{cp.channel}</span>
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    {cp.volume.toLocaleString()}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                        {cp.deliveryRate}%
                      </span>
                      <div style={{ width: '50px', height: '4px', background: 'var(--bg-muted)', borderRadius: '2px' }}>
                        <div style={{ width: `${cp.deliveryRate}%`, height: '100%', background: 'var(--success)', borderRadius: '2px' }} />
                      </div>
                    </div>
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {cp.readRate}%
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {cp.avgLatencyMs} ms
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    ${cp.totalCost.toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 24-Hour Diurnal Throughput Bar Curve */}
      <div className="card">
        <div className="card-header">
          <div>
            <div className="card-title">24-Hour Diurnal Dispatch Distribution</div>
            <div className="card-subtitle">Dispatch volume reflects Quiet Hours suppression (22:00-08:00) and morning recovery bursts</div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-end', gap: '14px', height: '130px', padding: '12px 8px 0' }}>
          {data?.hourlyThroughput?.map(item => {
            const heightPercent = Math.max(10, (item.sent / 4200) * 100);
            return (
              <div key={item.hour} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%', justifyContent: 'flex-end' }}>
                <div style={{ fontSize: '10.5px', fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                  {item.sent}
                </div>
                <div style={{ 
                  width: '100%', 
                  height: `${heightPercent}%`, 
                  backgroundColor: 'var(--primary)',
                  borderRadius: '2px 2px 0 0',
                  opacity: 0.85
                }} />
                <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '6px', fontFamily: 'var(--font-mono)' }}>
                  {item.hour}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
