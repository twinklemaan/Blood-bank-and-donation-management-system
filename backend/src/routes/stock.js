// routes/stock.js - Stock Aggregation & Hospital Availability Search (Unit 4: Aggregation & $lookup)
const express = require('express');
const mongoose = require('mongoose');
const Donation = require('../models/Donation');
const { authenticate, allow } = require('../middleware/auth');

const router = express.Router();

const ALL_BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

/**
 * Helper to ensure all 8 blood groups are represented, filling empty ones with 0 units.
 */
function fillMissingBloodGroups(aggregatedList) {
  const stockMap = {};
  ALL_BLOOD_GROUPS.forEach((bg) => (stockMap[bg] = 0));

  aggregatedList.forEach((item) => {
    if (stockMap.hasOwnProperty(item._id)) {
      stockMap[item._id] = item.units;
    }
  });

  return ALL_BLOOD_GROUPS.map((bg) => ({
    bloodGroup: bg,
    units: stockMap[bg],
  }));
}

/**
 * 1. GET /api/public/stock
 * Public: Aggregated totals across all blood banks for static site and Tomcat servlet
 */
router.get('/public/stock', async (req, res) => {
  try {
    const today = new Date();

    // Aggregation 1: $match -> $group -> $sort
    const aggregated = await Donation.aggregate([
      {
        $match: {
          screeningResult: 'Pass',
          unitsRemaining: { $gt: 0 },
          expiryDate: { $gte: today },
        },
      },
      {
        $group: {
          _id: '$bloodGroup',
          units: { $sum: '$unitsRemaining' },
        },
      },
      { $sort: { _id: 1 } },
    ]);

    const result = fillMissingBloodGroups(aggregated);
    return res.json(result);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 2. GET /api/stock
 * Staff: Aggregated stock for the staff member's own bank
 */
router.get('/stock', authenticate, allow('STAFF'), async (req, res) => {
  try {
    const today = new Date();
    const bankObjectId = new mongoose.Types.ObjectId(req.user.bankId);

    // Aggregation 1: $match bank, Pass, remaining > 0, not expired -> $group -> $sort
    const aggregated = await Donation.aggregate([
      {
        $match: {
          bankId: bankObjectId,
          screeningResult: 'Pass',
          unitsRemaining: { $gt: 0 },
          expiryDate: { $gte: today },
        },
      },
      {
        $group: {
          _id: '$bloodGroup',
          units: { $sum: '$unitsRemaining' },
        },
      },
      { $sort: { _id: 1 } },
    ]);

    const result = fillMissingBloodGroups(aggregated);
    return res.json(result);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 3. GET /api/availability?bloodGroup=&city=
 * Hospital: Cross-bank inventory search using Aggregation + $lookup on Banks collection
 */
router.get('/availability', authenticate, allow('HOSPITAL'), async (req, res) => {
  try {
    const { bloodGroup, city } = req.query;
    const today = new Date();

    const matchConditions = {
      screeningResult: 'Pass',
      unitsRemaining: { $gt: 0 },
      expiryDate: { $gte: today },
    };

    if (bloodGroup) {
      matchConditions.bloodGroup = bloodGroup;
    }

    // SYLLABUS CONCEPT: Aggregation Pipeline with $group, $lookup, $unwind, $match, and $project
    const pipeline = [
      { $match: matchConditions },
      {
        $group: {
          _id: { bankId: '$bankId', bloodGroup: '$bloodGroup' },
          availableUnits: { $sum: '$unitsRemaining' },
        },
      },
      {
        $lookup: {
          from: 'banks',
          localField: '_id.bankId',
          foreignField: '_id',
          as: 'bank',
        },
      },
      { $unwind: '$bank' },
    ];

    if (city) {
      pipeline.push({
        $match: { 'bank.city': { $regex: city, $options: 'i' } },
      });
    }

    pipeline.push({
      $project: {
        _id: 0,
        bankId: '$_id.bankId',
        bankName: '$bank.name',
        city: '$bank.city',
        phone: '$bank.phone',
        bloodGroup: '$_id.bloodGroup',
        availableUnits: 1,
      },
    });

    pipeline.push({
      $sort: { city: 1, bankName: 1, bloodGroup: 1 },
    });

    const results = await Donation.aggregate(pipeline);
    return res.json(results);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

/**
 * 4. GET /api/stock/donations-summary
 * Aggregation 2: Total donation count and units per blood group with $lookup for bank name
 */
router.get('/stock/donations-summary', authenticate, async (req, res) => {
  try {
    const summary = await Donation.aggregate([
      {
        $group: {
          _id: { bankId: '$bankId', bloodGroup: '$bloodGroup' },
          donationCount: { $sum: 1 },
          totalUnitsDonated: { $sum: '$units' },
        },
      },
      {
        $lookup: {
          from: 'banks',
          localField: '_id.bankId',
          foreignField: '_id',
          as: 'bank',
        },
      },
      { $unwind: '$bank' },
      {
        $project: {
          _id: 0,
          bankId: '$_id.bankId',
          bankName: '$bank.name',
          bloodGroup: '$_id.bloodGroup',
          donationCount: 1,
          totalUnitsDonated: 1,
        },
      },
      { $sort: { bankName: 1, bloodGroup: 1 } },
    ]);

    return res.json(summary);
  } catch (err) {
    return res.status(500).json({ message: err.message });
  }
});

module.exports = router;
