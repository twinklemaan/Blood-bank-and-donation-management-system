// routes/auth.js - Authentication Routes (Unit 6: Login, Registration, JWT)
const express = require('express');
const { body, validationResult } = require('express-validator');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/User');

const router = express.Router();

/**
 * POST /api/auth/register
 * Public: Allows hospitals to register an account
 */
router.post(
  '/register',
  [
    body('username')
      .trim()
      .isLength({ min: 3 })
      .withMessage('Username must be at least 3 characters long'),
    body('password')
      .isLength({ min: 6 })
      .withMessage('Password must be at least 6 characters long'),
    body('name').trim().notEmpty().withMessage('Hospital name is required'),
    body('city').trim().notEmpty().withMessage('City is required'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const { username, password, name, city } = req.body;

      // Check for duplicate username
      const existingUser = await User.findOne({ username });
      if (existingUser) {
        return res.status(409).json({ message: 'Username already taken' });
      }

      // Hash password using bcryptjs
      const salt = await bcrypt.genSalt(10);
      const passwordHash = await bcrypt.hash(password, salt);

      // Create new HOSPITAL user (staff accounts are provisioned via seed)
      const user = new User({
        username,
        passwordHash,
        role: 'HOSPITAL',
        name,
        city,
      });

      await user.save();

      return res.status(201).json({
        message: 'Hospital registered successfully',
        user: {
          id: user._id,
          username: user.username,
          name: user.name,
          role: user.role,
          city: user.city,
        },
      });
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

/**
 * POST /api/auth/login
 * Public: Authenticates staff or hospital users and issues a JWT
 */
router.post(
  '/login',
  [
    body('username').trim().notEmpty().withMessage('Username is required'),
    body('password').notEmpty().withMessage('Password is required'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const { username, password } = req.body;

      // Locate user by username
      const user = await User.findOne({ username });
      if (!user) {
        return res.status(401).json({ message: 'Invalid username or password' });
      }

      // Verify hashed password
      const isMatch = await bcrypt.compare(password, user.passwordHash);
      if (!isMatch) {
        return res.status(401).json({ message: 'Invalid username or password' });
      }

      // Generate signed JWT token
      const secret = process.env.JWT_SECRET || 'bloodbank_secret_key_123';
      const payload = {
        id: user._id,
        username: user.username,
        role: user.role,
        bankId: user.bankId,
        name: user.name,
        city: user.city,
      };

      const token = jwt.sign(payload, secret, { expiresIn: '8h' });

      return res.json({
        token,
        user: payload,
      });
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

module.exports = router;
