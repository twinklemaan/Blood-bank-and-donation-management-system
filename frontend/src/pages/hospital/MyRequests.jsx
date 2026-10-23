// pages/hospital/MyRequests.jsx - Hospital Request History (useFetch)
import React from 'react';
import { useFetch } from '../../hooks/useFetch';

export default function MyRequests() {
  const { data: requests, loading, error } = useFetch('/api/requests/mine');

  const getStatusBadge = (status) => {
    if (status === 'Fulfilled') {
      return <span className="btn-sm" style={{ background: '#38a169', color: '#fff', borderRadius: '4px' }}>Fulfilled</span>;
    }
    if (status === 'Rejected') {
      return <span className="btn-sm" style={{ background: '#e53e3e', color: '#fff', borderRadius: '4px' }}>Rejected</span>;
    }
    return <span className="btn-sm" style={{ background: '#d69e2e', color: '#fff', borderRadius: '4px' }}>Pending</span>;
  };

  return (
    <div>
      <div className="page-header">
        <h2>Your Hospital Blood Requests</h2>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p>Loading your requests history...</p>}

      <div className="card">
        <h3>Request Records</h3>
        {requests && requests.length === 0 && (
          <p style={{ marginTop: '1rem', color: '#718096' }}>No requests raised yet by your facility.</p>
        )}

        {requests && requests.length > 0 && (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Blood Bank</th>
                  <th>City</th>
                  <th>Blood Group</th>
                  <th>Units</th>
                  <th>Urgency</th>
                  <th>Status</th>
                  <th>Requested At</th>
                  <th>Allocations</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((r) => (
                  <tr key={r._id}>
                    <td><strong>{r.bankId?.name || 'Blood Bank'}</strong></td>
                    <td>{r.bankId?.city || 'N/A'}</td>
                    <td>
                      <span style={{ fontWeight: 800, color: 'var(--primary)' }}>{r.bloodGroup}</span>
                    </td>
                    <td>{r.unitsRequested} unit(s)</td>
                    <td>{r.urgency}</td>
                    <td>{getStatusBadge(r.status)}</td>
                    <td>{new Date(r.requestDate).toLocaleString()}</td>
                    <td>
                      {r.status === 'Fulfilled' && r.issues?.length > 0 ? (
                        <span style={{ fontSize: '0.8rem', color: '#2b6cb0' }}>
                          Fulfilled from {r.issues.length} donation batch(es)
                        </span>
                      ) : (
                        <span style={{ fontSize: '0.8rem', color: '#718096' }}>None</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
