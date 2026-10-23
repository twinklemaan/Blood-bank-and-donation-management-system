// pages/hospital/RaiseRequest.jsx - Raise Blood Request (useState, fetch)
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function RaiseRequest() {
  const { token, user } = useAuth();
  const navigate = useNavigate();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const [banks, setBanks] = useState([]);
  const [formData, setFormData] = useState({
    bankId: '',
    bloodGroup: 'O+',
    unitsRequested: 1,
    urgency: 'Normal',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Fetch available blood banks for selection
  useEffect(() => {
    fetch(`${baseUrl}/api/availability`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        if (Array.isArray(data)) {
          // Extract unique banks
          const uniqueBanks = [];
          const seenIds = new Set();
          data.forEach((item) => {
            if (!seenIds.has(item.bankId)) {
              seenIds.add(item.bankId);
              uniqueBanks.push({ _id: item.bankId, name: item.bankName, city: item.city });
            }
          });
          setBanks(uniqueBanks);
          if (uniqueBanks.length > 0) {
            setFormData((prev) => ({ ...prev, bankId: uniqueBanks[0]._id }));
          }
        }
      })
      .catch((err) => console.error('Failed to load banks list:', err));
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const res = await fetch(`${baseUrl}/api/requests`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(formData),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to submit blood request');
      }

      setSuccess('Blood request dispatched successfully to the selected blood bank!');
      setTimeout(() => navigate('/hospital/my-requests'), 1500);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '1rem auto' }}>
      <div className="page-header">
        <h2>Raise Emergency / Clinical Blood Request</h2>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Requesting Entity</label>
            <input type="text" className="form-input" disabled value={`${user?.name} (${user?.city})`} />
          </div>

          <div className="form-group">
            <label>Target Blood Bank *</label>
            <select
              className="form-select"
              required
              value={formData.bankId}
              onChange={(e) => setFormData({ ...formData, bankId: e.target.value })}
            >
              <option value="">-- Select Blood Bank --</option>
              {banks.map((b) => (
                <option key={b._id} value={b._id}>
                  {b.name} ({b.city})
                </option>
              ))}
            </select>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Blood Group Required *</label>
              <select
                className="form-select"
                value={formData.bloodGroup}
                onChange={(e) => setFormData({ ...formData, bloodGroup: e.target.value })}
              >
                {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => (
                  <option key={bg} value={bg}>{bg}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Units Needed *</label>
              <input
                type="number"
                min="1"
                max="20"
                className="form-input"
                required
                value={formData.unitsRequested}
                onChange={(e) => setFormData({ ...formData, unitsRequested: Number(e.target.value) })}
              />
            </div>
          </div>

          <div className="form-group">
            <label>Clinical Urgency Priority *</label>
            <select
              className="form-select"
              value={formData.urgency}
              onChange={(e) => setFormData({ ...formData, urgency: e.target.value })}
            >
              <option value="Normal">Normal (Routine scheduled procedure)</option>
              <option value="Urgent">Urgent (Surgery within 24 hours)</option>
              <option value="Critical">Critical (Immediate trauma / emergency resuscitation)</option>
            </select>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.75rem' }}
            disabled={loading}
          >
            {loading ? 'Submitting Request...' : 'Submit Blood Request'}
          </button>
        </form>
      </div>
    </div>
  );
}
