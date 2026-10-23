// routes/donors.js - Donor Management CRUD & Eligibility (Unit 4: MongoDB CRUD)
const express = require('express');
const { body, validationResult } = require('express-validator');
const Donor = require('../models/Donor');
const Donation = require('../models/Donation');
const { authenticate, allow } = require('../middleware/auth');
const rules = require('../rules');

const router = express.Router();

// Apply auth middleware to all donor routes (Staff only)
router.use(authenticate, allow('STAFF'));

/**
 * 1. CREATE: POST /api/donors
 * Register a new blood donor
 */
router.post(
  '/',
  [
    body('name').trim().notEmpty().withMessage('Donor name is required'),
    body('gender')
      .isIn(['Male', 'Female', 'Other'])
      .withMessage('Gender must be Male, Female, or Other'),
    body('dob').isISO8601().withMessage('Valid date of birth (YYYY-MM-DD) is required'),
    body('bloodGroup')
      .isIn(['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'])
      .withMessage('Valid blood group is required'),
    body('phones')
      .isArray({ min: 1 })
      .withMessage('At least one contact phone number is required'),
    body('address.city').trim().notEmpty().withMessage('Address city is required'),
    body('address.pincode')
      .matches(/^\d{6}$/)
      .withMessage('Pincode must be exactly 6 digits'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const { name, gender, dob, bloodGroup, phones, address } = req.body;

      // Validate donor age
      try {
        rules.checkDonorAge(dob, new Date());
      } catch (err) {
        return res.status(400).json({ message: err.message });
      }

      const donor = new Donor({
        name,
        gender,
        dob,
        bloodGroup,
        phones,
        address,
      });

      await donor.save();
      return res.status(201).json(donor);
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

/**
 * 2. READ ALL: GET /api/donors
 * Retrieve all donors (with optional search query)
 */
router.get('/', async (req, res) => {
  try {
    const { search } = req.query;
    let query = {};
    if (search) {
      query = {
        $or: [
          { name: { $regex: search, $options: 'i' } },
          { bloodGroup: search },
          { 'address.city': { $regex: search, $options: 'i' } },
        ],
      };
    }
    const donors = await Donor.find(query).sort({ createdAt: -1 });
    return res.json(donors);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 3. READ ONE: GET /api/donors/:id
 * Retrieve a specific donor by ID
 */
router.get('/:id', async (req, res) => {
  try {
    const donor = await Donor.findById(req.params.id);
    if (!donor) {
      return res.status(404).json({ message: 'Donor not found' });
    }
    return res.json(donor);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 4. ELIGIBILITY CHECK: GET /api/donors/:id/eligibility
 * Check if the donor is eligible to donate today
 */
router.get('/:id/eligibility', async (req, res) => {
  try {
    const donor = await Donor.findById(req.params.id);
    if (!donor) {
      return res.status(404).json({ message: 'Donor not found' });
    }

    const today = new Date();

    // Check age 18-65
    try {
      rules.checkDonorAge(donor.dob, today);
    } catch (err) {
      return res.json({
        eligible: false,
        reason: err.message,
        nextEligibleDate: null,
      });
    }

    // Find latest non-failed donation
    const lastDonation = await Donation.findOne({
      donorId: donor._id,
      screeningResult: { $ne: 'Fail' },
    }).sort({ donationDate: -1 });

    if (!lastDonation) {
      return res.json({
        eligible: true,
        reason: 'First-time or eligible donor',
        nextEligibleDate: null,
      });
    }

    // Check 90 days gap
    const nextEligibleDate = new Date(lastDonation.donationDate);
    nextEligibleDate.setDate(nextEligibleDate.getDate() + 90);

    if (today < nextEligibleDate) {
      return res.json({
        eligible: false,
        reason: 'Donor must wait 90 days between donations',
        nextEligibleDate,
      });
    }

    return res.json({
      eligible: true,
      reason: 'Eligible to donate',
      nextEligibleDate: null,
    });
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 5. UPDATE: PUT /api/donors/:id
 * Update donor profile details
 */
router.put(
  '/:id',
  [
    body('name').optional().trim().notEmpty().withMessage('Donor name cannot be empty'),
    body('gender')
      .optional()
      .isIn(['Male', 'Female', 'Other'])
      .withMessage('Invalid gender'),
    body('bloodGroup')
      .optional()
      .isIn(['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'])
      .withMessage('Invalid blood group'),
    body('phones')
      .optional()
      .isArray({ min: 1 })
      .withMessage('At least one phone is required'),
    body('address.city').optional().trim().notEmpty(),
    body('address.pincode')
      .optional()
      .matches(/^\d{6}$/)
      .withMessage('Pincode must be exactly 6 digits'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const donor = await Donor.findById(req.params.id);
      if (!donor) {
        return res.status(404).json({ message: 'Donor not found' });
      }

      // Business Rule: Blood group cannot change once donor has donations
      if (req.body.bloodGroup && req.body.bloodGroup !== donor.bloodGroup) {
        const donationCount = await Donation.countDocuments({ donorId: donor._id });
        if (donationCount > 0) {
          return res.status(400).json({
            message: 'Blood group cannot change once a donor has donations',
          });
        }
        donor.bloodGroup = req.body.bloodGroup;
      }

      if (req.body.name) donor.name = req.body.name;
      if (req.body.gender) donor.gender = req.body.gender;
      if (req.body.dob) donor.dob = req.body.dob;
      if (req.body.phones) donor.phones = req.body.phones;
      if (req.body.address) {
        if (req.body.address.city) donor.address.city = req.body.address.city;
        if (req.body.address.pincode) donor.address.pincode = req.body.address.pincode;
      }

      await donor.save();
      return res.json(donor);
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

/**
 * 6. DELETE: DELETE /api/donors/:id
 * Delete a donor ONLY if no donations exist for this donor
 */
router.delete('/:id', async (req, res) => {
  try {
    const donor = await Donor.findById(req.params.id);
    if (!donor) {
      return res.status(404).json({ message: 'Donor not found' });
    }

    const donationCount = await Donation.countDocuments({ donorId: donor._id });
    if (donationCount > 0) {
      return res.status(400).json({
        message: 'Cannot delete donor with existing donation history',
      });
    }

    await Donor.findByIdAndDelete(req.params.id);
    return res.json({ message: 'Donor deleted successfully' });
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

module.exports = router;
