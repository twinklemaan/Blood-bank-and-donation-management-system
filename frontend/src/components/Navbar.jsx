// components/Navbar.jsx - Role-based Navigation Bar (Functional Component & Props)
import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// SYLLABUS REQUIREMENT: Role-based menu defined from one declarative object
const MENU_ITEMS = {
  STAFF: [
    { label: 'Donors', path: '/staff/donors' },
    { label: 'Donations', path: '/staff/donations' },
    { label: 'Live Stock', path: '/staff/stock' },
    { label: 'Requests to Fulfil', path: '/staff/requests' },
  ],
  HOSPITAL: [
    { label: 'Raise Request', path: '/hospital/raise-request' },
    { label: 'My Requests', path: '/hospital/my-requests' },
    { label: 'Availability Search', path: '/hospital/availability' },
  ],
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navLinks = user && MENU_ITEMS[user.role] ? MENU_ITEMS[user.role] : [];

  return (
    <nav className="app-navbar">
      <NavLink to="/" className="brand-title">
        🩸 BloodBank Portal
      </NavLink>

      <ul className="nav-links">
        {user ? (
          <>
            {navLinks.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={({ isActive }) => (isActive ? 'active' : '')}
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
            <li>
              <span className="user-badge">
                {user.name} ({user.role})
              </span>
            </li>
            <li>
              <button onClick={handleLogout} className="btn btn-secondary btn-sm">
                Logout
              </button>
            </li>
          </>
        ) : (
          <>
            <li>
              <NavLink to="/login">Login</NavLink>
            </li>
            <li>
              <NavLink to="/register">Hospital Register</NavLink>
            </li>
          </>
        )}
      </ul>
    </nav>
  );
}
