// hooks/useFetch.js - Custom React Hook for Data Fetching (Syllabus CO: Custom Hooks)
import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';

export function useFetch(endpoint, options = {}) {
  const { token } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const executeFetch = useCallback(async () => {
    setLoading(true);
    setError(null);

    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(`${baseUrl}${endpoint}`, {
        ...options,
        headers,
      });

      const json = await response.json();

      if (!response.ok) {
        throw new Error(json.message || `Request failed with status ${response.status}`);
      }

      setData(json);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [endpoint, token]);

  useEffect(() => {
    executeFetch();
  }, [executeFetch]);

  return { data, loading, error, refetch: executeFetch };
}
