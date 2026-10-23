// routes/requests.js - Hospital Blood Requests & Staff FEFO Fulfillment (Unit 4 & 6)
const express = require('express');
const { body, validationResult } = require('express-validator');
const BloodRequest = require('../models/BloodRequest');
const Donation = require('../models/Donation');
const User = require('../models/User');
const { authenticate, allow } = require('../middleware/auth');
const rules = require('../rules');
const appEvents = require('../events');

const router = express.Router();

/**
 * 1. POST /api/requests
 * Hospital: Raise a blood request for a specific bank
 */
router.post(
  '/',
  authenticate,
  allow('HOSPITAL'),
  [
    body('bankId').isMongoId().withMessage('Valid bank ID is required'),
    body('bloodGroup')
      .isIn(['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'])
      .withMessage('Valid blood group is required'),
    body('unitsRequested')
      .isInt({ min: 1 })
      .withMessage('Units requested must be at least 1'),
    body('urgency')
      .optional()
      .isIn(['Normal', 'Urgent', 'Critical'])
      .withMessage('Urgency must be Normal, Urgent, or Critical'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: errors.array()[0].msg });
    }

    try {
      const { bankId, bloodGroup, unitsRequested, urgency = 'Normal' } = req.body;

      const bloodRequest = new BloodRequest({
        hospitalId: req.user.id,
        bankId,
        bloodGroup,
        unitsRequested: Number(unitsRequested),
        urgency,
        status: 'Pending',
        requestDate: new Date(),
        issues: [],
      });

      await bloodRequest.save();
      return res.status(201).json(bloodRequest);
    } catch (err) {
      return res.status(500).json({ message: err.message });
    }
  }
);

/**
 * 2. GET /api/requests/mine
 * Hospital: View all blood requests raised by the logged-in hospital
 */
router.get('/mine', authenticate, allow('HOSPITAL'), async (req, res) => {
  try {
    const requests = await BloodRequest.find({ hospitalId: req.user.id })
      .populate('bankId', 'name city phone')
      .populate('issues.donationId')
      .sort({ createdAt: -1 });

    return res.json(requests);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 3. GET /api/requests/to-fulfil
 * Staff: View pending requests for staff's bank, prioritized by Critical -> Urgent -> Normal
 */
router.get('/to-fulfil', authenticate, allow('STAFF'), async (req, res) => {
  try {
    const pendingRequests = await BloodRequest.find({
      bankId: req.user.bankId,
      status: 'Pending',
    }).populate('hospitalId', 'name city username');

    // Rule 9: Priority sorting
    const sortedRequests = rules.sortHospitalRequests(pendingRequests);
    return res.json(sortedRequests);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 4. POST /api/requests/:id/fulfil
 * Staff: Fulfil request using First-Expiry-First-Out (FEFO) logic (Rule 8)
 */
router.post('/:id/fulfil', authenticate, allow('STAFF'), async (req, res) => {
  try {
    const bloodRequest = await BloodRequest.findById(req.params.id);
    if (!bloodRequest) {
      return res.status(404).json({ message: 'Blood request not found' });
    }

    // Ensure staff belongs to the bank handling the request
    if (bloodRequest.bankId.toString() !== req.user.bankId.toString()) {
      return res.status(403).json({ message: 'Forbidden: Cannot fulfil requests of another bank' });
    }

    if (bloodRequest.status !== 'Pending') {
      return res.status(400).json({ message: 'Only Pending requests can be fulfilled' });
    }

    const today = new Date();

    // Query eligible stock: Pass units, remaining > 0, not expired, sorted by earliest expiry date (FEFO)
    const availableDonations = await Donation.find({
      bankId: req.user.bankId,
      bloodGroup: bloodRequest.bloodGroup,
      screeningResult: 'Pass',
      unitsRemaining: { $gt: 0 },
      expiryDate: { $gte: today },
    }).sort({ expiryDate: 1 });

    // Execute pure FEFO fulfillment algorithm from rules.js
    const outcome = rules.fulfilRequestFEFO(bloodRequest, availableDonations);

    if (!outcome.canFulfil) {
      // Insufficient stock -> Reject request
      bloodRequest.status = 'Rejected';
      await bloodRequest.save();

      // Emit event for fs log and mailer
      appEvents.emit('requestFulfilled', {
        request: bloodRequest,
        outcome: 'Rejected',
      });

      return res.json({
        message: 'Request rejected due to insufficient stock',
        request: bloodRequest,
      });
    }

    // Sufficient stock available -> Apply unit deductions in order
    for (const update of outcome.donationUpdates) {
      await Donation.findByIdAndUpdate(update._id, {
        unitsRemaining: update.unitsRemaining,
      });
    }

    bloodRequest.issues = outcome.issues;
    bloodRequest.status = 'Fulfilled';
    await bloodRequest.save();

    // Emit event for fs log and mailer
    appEvents.emit('requestFulfilled', {
      request: bloodRequest,
      outcome: 'Fulfilled',
    });

    return res.json({
      message: 'Request fulfilled successfully',
      request: bloodRequest,
    });
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

module.exports = router;
