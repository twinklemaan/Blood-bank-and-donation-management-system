// pages/staff/RequestsToFulfil.jsx - Staff Request Fulfillment (FEFO Dispatch)
import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useFetch } from '../../hooks/useFetch';

export default function RequestsToFulfil() {
  const { token } = useAuth();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const { data: requests, loading, error, refetch } = useFetch('/api/requests/to-fulfil');
  const [outcomeMessage, setOutcomeMessage] = useState(null);
  const [fulfillingId, setFulfillingId] = useState(null);

  const handleFulfil = async (requestId) => {
    setFulfillingId(requestId);
    setOutcomeMessage(null);

    try {
      const res = await fetch(`${baseUrl}/api/requests/${requestId}/fulfil`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Fulfillment execution failed');
      }

      setOutcomeMessage({
        type: data.request.status === 'Fulfilled' ? 'success' : 'error',
        text: data.message,
        details: data.request,
      });

      refetch();
    } catch (err) {
      setOutcomeMessage({ type: 'error', text: err.message });
    } finally {
      setFulfillingId(null);
    }
  };

  const getUrgencyBadge = (urgency) => {
    if (urgency === 'Critical') return <span className="btn-sm" style={{ background: '#e53e3e', color: '#fff', borderRadius: '4px' }}>Critical</span>;
    if (urgency === 'Urgent') return <span className="btn-sm" style={{ background: '#dd6b20', color: '#fff', borderRadius: '4px' }}>Urgent</span>;
    return <span className="btn-sm" style={{ background: '#718096', color: '#fff', borderRadius: '4px' }}>Normal</span>;
  };

  return (
    <div>
      <div className="page-header">
        <h2>Hospital Blood Requests to Fulfil</h2>
      </div>

      {outcomeMessage && (
        <div className={`alert ${outcomeMessage.type === 'success' ? 'alert-success' : 'alert-error'}`}>
          <strong>Outcome: </strong> {outcomeMessage.text}
          {outcomeMessage.details?.issues?.length > 0 && (
            <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
              Allocated units under FEFO from {outcomeMessage.details.issues.length} donation batch(es).
            </div>
          )}
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p>Loading pending hospital requests...</p>}

      <div className="card">
        <h3>Pending Requests Queue</h3>
        <p style={{ fontSize: '0.85rem', color: '#718096', marginBottom: '1rem' }}>
          Sorted automatically by clinical urgency: <strong>Critical &gt; Urgent &gt; Normal</strong>, followed by oldest request timestamp.
        </p>

        {requests && requests.length === 0 && (
          <p style={{ marginTop: '1rem', color: '#718096' }}>No pending requests currently awaiting fulfillment.</p>
        )}

        {requests && requests.length > 0 && (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Hospital Name</th>
                  <th>City</th>
                  <th>Urgency</th>
                  <th>Blood Group</th>
                  <th>Units Requested</th>
                  <th>Request Time</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((r) => (
                  <tr key={r._id}>
                    <td><strong>{r.hospitalId?.name || 'Hospital'}</strong></td>
                    <td>{r.hospitalId?.city || 'N/A'}</td>
                    <td>{getUrgencyBadge(r.urgency)}</td>
                    <td>
                      <span style={{ fontWeight: 800, color: 'var(--primary)', fontSize: '1.1rem' }}>
                        {r.bloodGroup}
                      </span>
                    </td>
                    <td><strong>{r.unitsRequested}</strong> units</td>
                    <td>{new Date(r.requestDate).toLocaleString()}</td>
                    <td>
                      <button
                        onClick={() => handleFulfil(r._id)}
                        className="btn btn-primary btn-sm"
                        disabled={fulfillingId === r._id}
                      >
                        {fulfillingId === r._id ? 'Processing...' : '⚡ Fulfil (FEFO)'}
                      </button>
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
