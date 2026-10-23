// pages/Register.jsx - Hospital Registration Page (useState, validation)
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

export default function Register() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    name: '',
    city: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const res = await fetch(`${baseUrl}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      setSuccess('Hospital registered successfully! Redirecting to login...');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '480px', margin: '2.5rem auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: '1.25rem', color: 'var(--primary)' }}>
        Hospital Registration
      </h2>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Hospital Name *</label>
          <input
            type="text"
            name="name"
            className="form-input"
            required
            value={formData.name}
            onChange={handleChange}
            placeholder="e.g. Sanjeevani Multi-Speciality Hospital"
          />
        </div>

        <div className="form-group">
          <label>City *</label>
          <input
            type="text"
            name="city"
            className="form-input"
            required
            value={formData.city}
            onChange={handleChange}
            placeholder="e.g. Pune"
          />
        </div>

        <div className="form-group">
          <label>Portal Username *</label>
          <input
            type="text"
            name="username"
            className="form-input"
            required
            minLength={3}
            value={formData.username}
            onChange={handleChange}
            placeholder="e.g. sanjeevani_admin"
          />
        </div>

        <div className="form-group">
          <label>Password (min 6 characters) *</label>
          <input
            type="password"
            name="password"
            className="form-input"
            required
            minLength={6}
            value={formData.password}
            onChange={handleChange}
            placeholder="••••••••"
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '0.5rem' }} disabled={loading}>
          {loading ? 'Registering...' : 'Register Hospital'}
        </button>
      </form>

      <p style={{ marginTop: '1.25rem', textAlign: 'center', fontSize: '0.85rem' }}>
        Already registered? <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 600 }}>Login here</Link>
      </p>
    </div>
  );
}
