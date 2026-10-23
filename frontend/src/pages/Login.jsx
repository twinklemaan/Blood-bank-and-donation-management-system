// pages/Login.jsx - Authentication Login Form (useState, fetch)
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await fetch(`${baseUrl}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Login failed');
      }

      login(data.token, data.user);

      if (data.user.role === 'STAFF') {
        navigate('/staff/donors');
      } else {
        navigate('/hospital/raise-request');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '420px', margin: '3rem auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: '1.25rem', color: 'var(--primary)' }}>
        User Login
      </h2>

      {error && <div className="alert alert-error">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Username</label>
          <input
            type="text"
            className="form-input"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="staff1 or hosp1"
          />
        </div>

        <div className="form-group">
          <label>Password</label>
          <input
            type="password"
            className="form-input"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '0.5rem' }} disabled={loading}>
          {loading ? 'Logging in...' : 'Sign In'}
        </button>
      </form>

      <p style={{ marginTop: '1.25rem', textAlign: 'center', fontSize: '0.85rem' }}>
        New hospital? <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 600 }}>Register here</Link>
      </p>

      <div style={{ marginTop: '1.5rem', background: '#f8f9fa', padding: '0.75rem', borderRadius: '4px', fontSize: '0.8rem' }}>
        <strong>Demo Credentials:</strong>
        <div>Staff Pune: <code>staff1</code> / <code>staff123</code></div>
        <div>Staff Mumbai: <code>staff2</code> / <code>staff123</code></div>
        <div>Hospital: <code>hosp1</code> / <code>hosp123</code></div>
      </div>
    </div>
  );
}
