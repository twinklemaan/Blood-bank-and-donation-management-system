// App.jsx - Main Application Component & Client Routing (React Router)
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';

import Login from './pages/Login';
import Register from './pages/Register';

// Staff Pages
import Donors from './pages/staff/Donors';
import Donations from './pages/staff/Donations';
import Stock from './pages/staff/Stock';
import RequestsToFulfil from './pages/staff/RequestsToFulfil';

// Hospital Pages
import RaiseRequest from './pages/hospital/RaiseRequest';
import MyRequests from './pages/hospital/MyRequests';
import AvailabilitySearch from './pages/hospital/AvailabilitySearch';

function RootRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role === 'STAFF') return <Navigate to="/staff/donors" replace />;
  return <Navigate to="/hospital/raise-request" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="app-container">
          <Navbar />
          <main className="main-content">
            <Routes>
              {/* Root Redirect */}
              <Route path="/" element={<RootRedirect />} />

              {/* Public Auth Routes */}
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              {/* Staff Routes */}
              <Route
                path="/staff/donors"
                element={
                  <ProtectedRoute allowedRole="STAFF">
                    <Donors />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/staff/donations"
                element={
                  <ProtectedRoute allowedRole="STAFF">
                    <Donations />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/staff/stock"
                element={
                  <ProtectedRoute allowedRole="STAFF">
                    <Stock />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/staff/requests"
                element={
                  <ProtectedRoute allowedRole="STAFF">
                    <RequestsToFulfil />
                  </ProtectedRoute>
                }
              />

              {/* Hospital Routes */}
              <Route
                path="/hospital/raise-request"
                element={
                  <ProtectedRoute allowedRole="HOSPITAL">
                    <RaiseRequest />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/hospital/my-requests"
                element={
                  <ProtectedRoute allowedRole="HOSPITAL">
                    <MyRequests />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/hospital/availability"
                element={
                  <ProtectedRoute allowedRole="HOSPITAL">
                    <AvailabilitySearch />
                  </ProtectedRoute>
                }
              />

              {/* Catch-all */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
