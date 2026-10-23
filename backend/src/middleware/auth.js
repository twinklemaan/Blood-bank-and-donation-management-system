// middleware/auth.js - JWT Authentication & Role Authorization Middleware (Unit 6: Auth)
const jwt = require('jsonwebtoken');

/**
 * Authentication middleware: verifies Bearer token in Authorization header
 */
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ message: 'Access denied: No token provided' });
  }

  const token = authHeader.split(' ')[1];
  const secret = process.env.JWT_SECRET || 'bloodbank_secret_key_123';

  try {
    const decoded = jwt.verify(token, secret);
    req.user = decoded; // { id, username, role, bankId, name, city }
    next();
  } catch (error) {
    return res.status(401).json({ message: 'Invalid or expired token' });
  }
}

/**
 * Role-based authorization helper: allow('STAFF') or allow('STAFF', 'HOSPITAL')
 */
function allow(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ message: 'Forbidden: Insufficient permissions' });
    }
    next();
  };
}

module.exports = {
  authenticate,
  allow,
};
