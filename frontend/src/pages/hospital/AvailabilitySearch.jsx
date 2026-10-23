// pages/hospital/AvailabilitySearch.jsx - Cross-Bank Search (useState, fetch)
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function AvailabilitySearch() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const [bloodGroup, setBloodGroup] = useState('');
  const [city, setCity] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const searchAvailability = async () => {
    setLoading(true);
    setError('');

    const queryParams = new URLSearchParams();
    if (bloodGroup) queryParams.append('bloodGroup', bloodGroup);
    if (city) queryParams.append('city', city);

    try {
      const res = await fetch(`${baseUrl}/api/availability?${queryParams.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Search failed');
      }

      setResults(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    searchAvailability();
  }, []);

  return (
    <div>
      <div className="page-header">
        <h2>Cross-Bank Inventory Availability Search</h2>
      </div>

      {/* Filter Bar */}
      <div className="card">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            searchAvailability();
          }}
        >
          <div className="form-row">
            <div className="form-group">
              <label>Filter by Blood Group</label>
              <select
                className="form-select"
                value={bloodGroup}
                onChange={(e) => setBloodGroup(e.target.value)}
              >
                <option value="">-- All Blood Groups --</option>
                {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => (
                  <option key={bg} value={bg}>{bg}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Filter by City</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Pune or Mumbai"
                value={city}
                onChange={(e) => setCity(e.target.value)}
              />
            </div>

            <div className="form-group" style={{ display: 'flex', alignItems: 'flex-end' }}>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
                🔍 Search Inventory
              </button>
            </div>
          </div>
        </form>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p>Querying real-time bank inventory...</p>}

      {/* Results Table */}
      <div className="card">
        <h3>Available Blood Stocks Across Regional Banks</h3>
        {results.length === 0 && !loading && (
          <p style={{ marginTop: '1rem', color: '#718096' }}>
            No matching blood stock found with selected criteria.
          </p>
        )}

        {results.length > 0 && (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Blood Group</th>
                  <th>Available Units</th>
                  <th>Blood Bank</th>
                  <th>City</th>
                  <th>Contact Phone</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {results.map((item, idx) => (
                  <tr key={`${item.bankId}-${item.bloodGroup}-${idx}`}>
                    <td>
                      <span style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--primary)' }}>
                        {item.bloodGroup}
                      </span>
                    </td>
                    <td>
                      <strong style={{ fontSize: '1.1rem' }}>{item.availableUnits}</strong> units
                    </td>
                    <td><strong>{item.bankName}</strong></td>
                    <td>{item.city}</td>
                    <td>{item.phone}</td>
                    <td>
                      <button
                        onClick={() => navigate('/hospital/raise-request')}
                        className="btn btn-primary btn-sm"
                      >
                        Request Blood
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
