// routes/donations.js - Donation Intake & Screening Workflow (Unit 4 & 6)
const express = require('express');
const { body, validationResult } = require('express-validator');
const fs = require('fs');
const path = require('path');
const Donation = require('../models/Donation');
const Donor = require('../models/Donor');
const { authenticate, allow } = require('../middleware/auth');
const upload = require('../middleware/upload');
const rules = require('../rules');

const router = express.Router();

// Staff authentication required for all donation routes
router.use(authenticate, allow('STAFF'));

/**
 * POST /api/donations
 * Record a new blood donation intake
 */
router.post(
  '/',
  [
    body('donorId').isMongoId().withMessage('Valid donor ID is required'),
    body('units')
      .isInt({ min: 1 })
      .withMessage('Units must be an integer of at least 1'),
    body('hemoglobin')
      .isFloat({ min: 0 })
      .withMessage('Valid hemoglobin value is required'),
    body('donationDate')
      .optional()
      .isISO8601()
      .withMessage('Donation date must be valid ISO8601 date'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const { donorId, units, hemoglobin } = req.body;
      const donationDate = req.body.donationDate ? new Date(req.body.donationDate) : new Date();

      // Rule 3: Donation date cannot be in the future
      try {
        rules.checkDonationDate(donationDate);
      } catch (err) {
        return res.status(400).json({ message: err.message });
      }

      // Rule 2: Hemoglobin must be >= 12.5
      try {
        rules.checkHemoglobin(hemoglobin);
      } catch (err) {
        return res.status(400).json({ message: err.message });
      }

      // Fetch donor
      const donor = await Donor.findById(donorId);
      if (!donor) {
        return res.status(404).json({ message: 'Donor not found' });
      }

      // Rule 1: Donor age on donation date must be 18-65
      try {
        rules.checkDonorAge(donor.dob, donationDate);
      } catch (err) {
        return res.status(400).json({ message: err.message });
      }

      // Rule 4: 90 days gap since last non-failed donation
      const lastDonation = await Donation.findOne({
        donorId: donor._id,
        screeningResult: { $ne: 'Fail' },
      }).sort({ donationDate: -1 });

      if (lastDonation) {
        try {
          rules.checkDonationGap(lastDonation.donationDate, donationDate);
        } catch (err) {
          return res.status(400).json({ message: err.message });
        }
      }

      // Rule 5: Calculate expiryDate (+35 days) and unitsRemaining
      const expiryDate = rules.calculateExpiryDate(donationDate);

      // Staff can only create donations for their own bank
      const donation = new Donation({
        donorId: donor._id,
        bankId: req.user.bankId,
        bloodGroup: donor.bloodGroup,
        donationDate,
        expiryDate,
        units: Number(units),
        unitsRemaining: Number(units),
        hemoglobin: Number(hemoglobin),
        screeningResult: 'Pending',
      });

      await donation.save();
      return res.status(201).json(donation);
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

/**
 * GET /api/donations
 * Retrieve all donations for the logged-in staff member's bank
 */
router.get('/', async (req, res) => {
  try {
    const donations = await Donation.find({ bankId: req.user.bankId })
      .populate('donorId', 'name gender bloodGroup dob')
      .sort({ donationDate: -1 });
    return res.json(donations);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * PATCH /api/donations/:id/screening
 * Update screening result (Pending -> Pass or Pending -> Fail)
 */
router.patch(
  '/:id/screening',
  [
    body('result')
      .isIn(['Pass', 'Fail'])
      .withMessage('Result must be either Pass or Fail'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const donation = await Donation.findById(req.params.id);
      if (!donation) {
        return res.status(404).json({ message: 'Donation not found' });
      }

      // Ensure staff belongs to the same bank as the donation
      if (donation.bankId.toString() !== req.user.bankId.toString()) {
        return res.status(403).json({ message: 'Forbidden: Cannot screen donations of another bank' });
      }

      const { result } = req.body;

      // Rule 6: Validate screening transition
      try {
        rules.validateScreeningTransition(donation.screeningResult, result);
      } catch (err) {
        return res.status(400).json({ message: err.message });
      }

      donation.screeningResult = result;

      // If failed screening, units become 0 and cannot be used
      if (result === 'Fail') {
        donation.unitsRemaining = 0;
      }

      await donation.save();
      return res.json(donation);
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

/**
 * POST /api/donations/:id/report
 * Staff: Upload a lab test report (PDF/PNG/JPG, max 2MB)
 * Deletes old report if replacing an existing one (SYLLABUS CONCEPT: fs.unlink)
 */
router.post('/:id/report', upload.single('report'), async (req, res) => {
  try {
    const donation = await Donation.findById(req.params.id);
    if (!donation) {
      // If upload happened but donation not found, clean up uploaded file
      if (req.file) {
        fs.unlink(req.file.path, () => {});
      }
      return res.status(404).json({ message: 'Donation not found' });
    }

    if (donation.bankId.toString() !== req.user.bankId.toString()) {
      if (req.file) {
        fs.unlink(req.file.path, () => {});
      }
      return res.status(403).json({ message: 'Forbidden: Cannot upload reports for another bank' });
    }

    if (!req.file) {
      return res.status(400).json({ message: 'Please upload a lab report file' });
    }

    // If donation already has an existing report file, remove it using fs.unlink
    if (donation.reportFile) {
      const oldFilePath = path.join(__dirname, '../../uploads', donation.reportFile);
      fs.unlink(oldFilePath, (err) => {
        if (err && err.code !== 'ENOENT') {
          console.warn('Could not delete old report file:', err.message);
        }
      });
    }

    // Save filename in database
    donation.reportFile = req.file.filename;
    await donation.save();

    return res.json({
      message: 'Lab report uploaded successfully',
      reportFile: donation.reportFile,
      downloadUrl: `/uploads/${donation.reportFile}`,
    });
  } catch (err) {
    if (req.file) {
      fs.unlink(req.file.path, () => {});
    }
    return res.status(500).json({ message: err.message });
  }
});

module.exports = router;
