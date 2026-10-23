// Donation.js - Mongoose Model for Blood Donations (Unit 4: Data modelling)
const mongoose = require('mongoose');

const donationSchema = new mongoose.Schema(
  {
    donorId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Donor',
      required: true,
    },
    bankId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Bank',
      required: true,
    },
    bloodGroup: {
      type: String,
      required: true,
      enum: ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'],
    },
    donationDate: {
      type: Date,
      default: Date.now,
      required: true,
    },
    // Expiry date is automatically set to donationDate + 35 days
    expiryDate: {
      type: Date,
      required: true,
    },
    units: {
      type: Number,
      required: true,
      min: [1, 'Donation must be at least 1 unit'],
    },
    unitsRemaining: {
      type: Number,
      required: true,
      min: [0, 'Remaining units cannot be negative'],
    },
    hemoglobin: {
      type: Number,
      required: true,
    },
    screeningResult: {
      type: String,
      enum: ['Pending', 'Pass', 'Fail'],
      default: 'Pending',
    },
    reportFile: {
      type: String,
      default: null,
    },
  },
  { timestamps: true }
);

// Index 2 of 3: Compound index to rapidly filter available stock by bank, blood group, screening status, and expiry date (FEFO query optimization)
donationSchema.index({ bankId: 1, bloodGroup: 1, screeningResult: 1, expiryDate: 1 });

module.exports = mongoose.model('Donation', donationSchema);
