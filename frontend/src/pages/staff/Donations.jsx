// pages/staff/Donations.jsx - Donation Intake, Lab Screening & Report Upload (Unit 3, 4 & 6)
import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useFetch } from '../../hooks/useFetch';

export default function Donations() {
  const { token } = useAuth();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const { data: donations, loading, error, refetch } = useFetch('/api/donations');
  const { data: donors } = useFetch('/api/donors');

  const [formData, setFormData] = useState({
    donorId: '',
    units: 1,
    hemoglobin: 13.5,
  });

  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');
  const [selectedFile, setSelectedFile] = useState({});
  const [uploadLoading, setUploadLoading] = useState(false);

  const handleRecordDonation = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormSuccess('');

    try {
      const res = await fetch(`${baseUrl}/api/donations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(formData),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to record donation');
      }

      setFormSuccess('Donation intake recorded successfully as Pending screening!');
      setFormData({ donorId: '', units: 1, hemoglobin: 13.5 });
      refetch();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleScreening = async (donationId, result) => {
    if (!window.confirm(`Mark screening result as ${result}? This action is final.`)) return;

    try {
      const res = await fetch(`${baseUrl}/api/donations/${donationId}/screening`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ result }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Screening update failed');
      }

      refetch();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleFileUpload = async (donationId) => {
    const file = selectedFile[donationId];
    if (!file) {
      alert('Please select a file first.');
      return;
    }

    setUploadLoading(true);
    const uploadData = new FormData();
    uploadData.append('report', file);

    try {
      const res = await fetch(`${baseUrl}/api/donations/${donationId}/report`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: uploadData,
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'File upload failed');
      }

      alert('Lab report uploaded successfully!');
      setSelectedFile({ ...selectedFile, [donationId]: null });
      refetch();
    } catch (err) {
      alert(err.message);
    } finally {
      setUploadLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Donation Intake & Quality Screening</h2>
      </div>

      {formError && <div className="alert alert-error">{formError}</div>}
      {formSuccess && <div className="alert alert-success">{formSuccess}</div>}

      {/* Record Donation Intake Form */}
      <div className="card">
        <h3>🩸 Record New Blood Donation</h3>
        <form onSubmit={handleRecordDonation} style={{ marginTop: '1rem' }}>
          <div className="form-row">
            <div className="form-group" style={{ flex: 2 }}>
              <label>Select Registered Donor *</label>
              <select
                className="form-select"
                required
                value={formData.donorId}
                onChange={(e) => setFormData({ ...formData, donorId: e.target.value })}
              >
                <option value="">-- Choose Donor --</option>
                {Array.isArray(donors) &&
                  donors.map((d) => (
                    <option key={d._id} value={d._id}>
                      {d.name} ({d.bloodGroup}) - {d.address?.city}
                    </option>
                  ))}
              </select>
            </div>

            <div className="form-group">
              <label>Units *</label>
              <input
                type="number"
                min="1"
                max="5"
                className="form-input"
                required
                value={formData.units}
                onChange={(e) => setFormData({ ...formData, units: Number(e.target.value) })}
              />
            </div>

            <div className="form-group">
              <label>Hemoglobin (Hb ≥ 12.5) *</label>
              <input
                type="number"
                step="0.1"
                min="5"
                max="25"
                className="form-input"
                required
                value={formData.hemoglobin}
                onChange={(e) => setFormData({ ...formData, hemoglobin: Number(e.target.value) })}
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
            Record Intake (Starts as Pending)
          </button>
        </form>
      </div>

      {/* Donations List */}
      <div className="card">
        <h3>Donations at Your Blood Bank</h3>
        {loading && <p>Loading donations...</p>}
        {error && <div className="alert alert-error">{error}</div>}

        {Array.isArray(donations) && donations.length === 0 && <p style={{ marginTop: '1rem' }}>No donations recorded yet.</p>}

        {Array.isArray(donations) && donations.length > 0 && (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Donor</th>
                  <th>Blood Group</th>
                  <th>Intake Date</th>
                  <th>Expiry Date</th>
                  <th>Stock (Rem/Total)</th>
                  <th>Hb Level</th>
                  <th>Screening Result</th>
                  <th>Lab Report File</th>
                </tr>
              </thead>
              <tbody>
                {donations.map((item) => (
                  <tr key={item._id}>
                    <td><strong>{item.donorId?.name || 'Anonymous'}</strong></td>
                    <td><span style={{ fontWeight: 700, color: 'var(--primary)' }}>{item.bloodGroup}</span></td>
                    <td>{new Date(item.donationDate).toLocaleDateString()}</td>
                    <td>{new Date(item.expiryDate).toLocaleDateString()}</td>
                    <td>
                      <strong>{item.unitsRemaining}</strong> / {item.units} units
                    </td>
                    <td>{item.hemoglobin} g/dL</td>
                    <td>
                      {item.screeningResult === 'Pending' ? (
                        <div style={{ display: 'flex', gap: '0.35rem' }}>
                          <button
                            onClick={() => handleScreening(item._id, 'Pass')}
                            className="btn btn-success btn-sm"
                          >
                            Pass
                          </button>
                          <button
                            onClick={() => handleScreening(item._id, 'Fail')}
                            className="btn btn-danger btn-sm"
                          >
                            Fail
                          </button>
                        </div>
                      ) : (
                        <span
                          className={`user-badge`}
                          style={{
                            backgroundColor: item.screeningResult === 'Pass' ? '#c6f6d5' : '#fed7d7',
                            color: item.screeningResult === 'Pass' ? '#22543d' : '#9b2c2c',
                          }}
                        >
                          {item.screeningResult}
                        </span>
                      )}
                    </td>
                    <td>
                      {item.reportFile ? (
                        <div>
                          <a
                            href={`${baseUrl}/uploads/${item.reportFile}`}
                            target="_blank"
                            rel="noreferrer"
                            style={{ color: 'var(--primary)', fontWeight: 600, fontSize: '0.85rem' }}
                          >
                            📄 View Report
                          </a>
                        </div>
                      ) : null}

                      <div style={{ marginTop: '0.25rem', display: 'flex', gap: '0.25rem', alignItems: 'center' }}>
                        <input
                          type="file"
                          accept=".pdf,.png,.jpg,.jpeg"
                          style={{ fontSize: '0.75rem', maxWidth: '140px' }}
                          onChange={(e) =>
                            setSelectedFile({ ...selectedFile, [item._id]: e.target.files[0] })
                          }
                        />
                        <button
                          onClick={() => handleFileUpload(item._id)}
                          className="btn btn-secondary btn-sm"
                          disabled={uploadLoading}
                        >
                          Upload
                        </button>
                      </div>
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
