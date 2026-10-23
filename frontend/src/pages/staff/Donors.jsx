// pages/staff/Donors.jsx - Donor Full CRUD & Eligibility Hint (useState, useEffect, useFetch)
import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useFetch } from '../../hooks/useFetch';

export default function Donors() {
  const { token } = useAuth();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const { data: donors, loading, error, refetch } = useFetch('/api/donors');

  const [formMode, setFormMode] = useState('ADD'); // 'ADD' or 'EDIT'
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    gender: 'Male',
    dob: '',
    bloodGroup: 'O+',
    phones: '',
    city: '',
    pincode: '',
  });

  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');
  const [eligibilityHint, setEligibilityHint] = useState(null);

  const resetForm = () => {
    setFormMode('ADD');
    setEditingId(null);
    setFormData({
      name: '',
      gender: 'Male',
      dob: '',
      bloodGroup: 'O+',
      phones: '',
      city: '',
      pincode: '',
    });
    setFormError('');
  };

  const handleEditClick = (donor) => {
    setFormMode('EDIT');
    setEditingId(donor._id);
    setFormData({
      name: donor.name,
      gender: donor.gender,
      dob: donor.dob ? donor.dob.split('T')[0] : '',
      bloodGroup: donor.bloodGroup,
      phones: donor.phones ? donor.phones.join(', ') : '',
      city: donor.address ? donor.address.city : '',
      pincode: donor.address ? donor.address.pincode : '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormSuccess('');

    const phoneArray = formData.phones.split(',').map((p) => p.trim()).filter(Boolean);

    const payload = {
      name: formData.name,
      gender: formData.gender,
      dob: formData.dob,
      bloodGroup: formData.bloodGroup,
      phones: phoneArray,
      address: {
        city: formData.city,
        pincode: formData.pincode,
      },
    };

    try {
      const url = formMode === 'ADD' ? `${baseUrl}/api/donors` : `${baseUrl}/api/donors/${editingId}`;
      const method = formMode === 'ADD' ? 'POST' : 'PUT';

      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      const resData = await res.json();
      if (!res.ok) {
        throw new Error(resData.message || 'Operation failed');
      }

      setFormSuccess(formMode === 'ADD' ? 'Donor registered successfully!' : 'Donor profile updated!');
      resetForm();
      refetch();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleDelete = async (donorId) => {
    if (!window.confirm('Are you sure you want to delete this donor?')) return;
    setFormError('');

    try {
      const res = await fetch(`${baseUrl}/api/donors/${donorId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to delete donor');
      }

      refetch();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const checkEligibility = async (donorId) => {
    try {
      const res = await fetch(`${baseUrl}/api/donors/${donorId}/eligibility`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setEligibilityHint({ donorId, ...data });
    } catch (err) {
      alert('Could not determine eligibility: ' + err.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Donor Management (Full CRUD)</h2>
      </div>

      {formError && <div className="alert alert-error">{formError}</div>}
      {formSuccess && <div className="alert alert-success">{formSuccess}</div>}

      {/* Donor Intake / Edit Form */}
      <div className="card">
        <h3>{formMode === 'ADD' ? '➕ Register New Donor' : '✏️ Edit Donor Profile'}</h3>
        <form onSubmit={handleSubmit} style={{ marginTop: '1rem' }}>
          <div className="form-row">
            <div className="form-group">
              <label>Full Name *</label>
              <input
                type="text"
                className="form-input"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. Anand Shinde"
              />
            </div>

            <div className="form-group">
              <label>Gender *</label>
              <select
                className="form-select"
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
              >
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>

            <div className="form-group">
              <label>Date of Birth *</label>
              <input
                type="date"
                className="form-input"
                required
                value={formData.dob}
                onChange={(e) => setFormData({ ...formData, dob: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label>Blood Group *</label>
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
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Phone Numbers (comma-separated) *</label>
              <input
                type="text"
                className="form-input"
                required
                value={formData.phones}
                onChange={(e) => setFormData({ ...formData, phones: e.target.value })}
                placeholder="e.g. 9822011111, 9822011112"
              />
            </div>

            <div className="form-group">
              <label>City *</label>
              <input
                type="text"
                className="form-input"
                required
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                placeholder="e.g. Pune"
              />
            </div>

            <div className="form-group">
              <label>Pincode (6 digits) *</label>
              <input
                type="text"
                className="form-input"
                required
                pattern="^\d{6}$"
                maxLength={6}
                value={formData.pincode}
                onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
                placeholder="e.g. 411001"
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem' }}>
            <button type="submit" className="btn btn-primary">
              {formMode === 'ADD' ? 'Save Donor' : 'Update Profile'}
            </button>
            {formMode === 'EDIT' && (
              <button type="button" onClick={resetForm} className="btn btn-secondary">
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Donors Table */}
      <div className="card">
        <h3>Registered Donors Directory</h3>
        {loading && <p>Loading donors...</p>}
        {error && <div className="alert alert-error">{error}</div>}

        {donors && donors.length === 0 && <p style={{ marginTop: '1rem' }}>No donors found in the database.</p>}

        {donors && donors.length > 0 && (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Blood Group</th>
                  <th>Gender</th>
                  <th>Age</th>
                  <th>Location</th>
                  <th>Phones</th>
                  <th>Eligibility</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {donors.map((d) => {
                  const birthYear = d.dob ? new Date(d.dob).getFullYear() : null;
                  const approxAge = birthYear ? new Date().getFullYear() - birthYear : 'N/A';
                  return (
                    <tr key={d._id}>
                      <td><strong>{d.name}</strong></td>
                      <td><span style={{ fontWeight: 700, color: 'var(--primary)' }}>{d.bloodGroup}</span></td>
                      <td>{d.gender}</td>
                      <td>{approxAge} yrs</td>
                      <td>{d.address ? `${d.address.city} (${d.address.pincode})` : 'N/A'}</td>
                      <td>{d.phones ? d.phones.join(', ') : ''}</td>
                      <td>
                        <button
                          onClick={() => checkEligibility(d._id)}
                          className="btn btn-secondary btn-sm"
                        >
                          Check Status
                        </button>
                        {eligibilityHint && eligibilityHint.donorId === d._id && (
                          <div style={{ fontSize: '0.8rem', marginTop: '0.25rem', color: eligibilityHint.eligible ? 'var(--success)' : 'var(--danger)' }}>
                            {eligibilityHint.eligible ? '✅ Eligible to donate' : `❌ ${eligibilityHint.reason}`}
                            {eligibilityHint.nextEligibleDate && (
                              <div>Next: {new Date(eligibilityHint.nextEligibleDate).toLocaleDateString()}</div>
                            )}
                          </div>
                        )}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                          <button onClick={() => handleEditClick(d)} className="btn btn-secondary btn-sm">
                            Edit
                          </button>
                          <button onClick={() => handleDelete(d._id)} className="btn btn-danger btn-sm">
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
