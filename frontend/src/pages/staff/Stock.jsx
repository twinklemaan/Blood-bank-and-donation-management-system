// pages/staff/Stock.jsx - Live Bank Stock with 10s Auto-Refresh (useEffect, setInterval)
import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';

export default function Stock() {
  const { token, user } = useAuth();
  const [stockList, setStockList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState(new Date());

  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const fetchStock = async () => {
    try {
      const res = await fetch(`${baseUrl}/api/stock`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message || 'Failed to fetch stock');
      setStockList(data);
      setLastUpdated(new Date());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // SYLLABUS REQUIREMENT: Auto-refresh every 10 seconds using setInterval in useEffect
  useEffect(() => {
    fetchStock();

    const intervalId = setInterval(() => {
      fetchStock();
    }, 10000); // 10 seconds

    return () => clearInterval(intervalId); // Cleanup timer on unmount
  }, [token]);

  const totalUnits = stockList.reduce((sum, item) => sum + item.units, 0);

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Live Blood Bank Stock</h2>
          <p style="color: #666; font-size: 0.9rem;">
            Bank Inventory for: <strong>{user?.name || 'Your Bank'}</strong>
          </p>
        </div>
        <div style={{ textAlign: 'right' }}>
          <button onClick={fetchStock} className="btn btn-secondary btn-sm">
            🔄 Refresh Now
          </button>
          <div style={{ fontSize: '0.8rem', color: '#718096', marginTop: '0.25rem' }}>
            Auto-refreshes every 10s • Last: {lastUpdated.toLocaleTimeString()}
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p>Loading real-time stock levels...</p>}

      <div className="card" style={{ marginBottom: '1.5rem', background: '#fdfdfe' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem' }}>Total Usable Inventory</h3>
            <p style={{ fontSize: '0.85rem', color: '#718096' }}>
              Only screened 'Pass' and non-expired units are counted.
            </p>
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)' }}>
            {totalUnits} <span style={{ fontSize: '1rem', fontWeight: 500 }}>Units</span>
          </div>
        </div>
      </div>

      {/* SYLLABUS REQUIREMENT: 8 Blood Groups, highlighted Red when <= 5 units */}
      <div className="stock-grid">
        {stockList.map((item) => {
          const isCritical = item.units <= 5;
          return (
            <div
              key={item.bloodGroup}
              className={`stock-card ${isCritical ? 'critical' : ''}`}
            >
              <div className="group">{item.bloodGroup}</div>
              <div className="units">{item.units}</div>
              <div style={{ fontSize: '0.8rem', marginTop: '0.25rem', fontWeight: 600 }}>
                {isCritical ? (
                  <span style={{ color: 'var(--danger)' }}>⚠️ Low Stock (≤ 5)</span>
                ) : (
                  <span style={{ color: 'var(--success)' }}>✅ Adequate</span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
