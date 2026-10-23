// BloodRequest.js - Mongoose Model for Hospital Blood Requests (Unit 4: Data modelling)
const mongoose = require('mongoose');

const bloodRequestSchema = new mongoose.Schema(
  {
    hospitalId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
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
    unitsRequested: {
      type: Number,
      required: true,
      min: [1, 'Request must ask for at least 1 unit'],
    },
    urgency: {
      type: String,
      enum: ['Normal', 'Urgent', 'Critical'],
      default: 'Normal',
    },
    status: {
      type: String,
      enum: ['Pending', 'Fulfilled', 'Rejected'],
      default: 'Pending',
    },
    requestDate: {
      type: Date,
      default: Date.now,
    },
    // DATA MODELLING CONCEPT: Many-to-Many relationship (Referenced)
    // One blood request can draw units from multiple donations, and one donation can serve multiple requests.
    // Stored as an array of sub-references capturing which donation contributed how many units.
    issues: [
      {
        donationId: {
          type: mongoose.Schema.Types.ObjectId,
          ref: 'Donation',
          required: true,
        },
        unitsTaken: {
          type: Number,
          required: true,
          min: 1,
        },
      },
    ],
  },
  { timestamps: true }
);

// Index 3 of 3: Compound index to quickly fetch pending or historical requests by bank
bloodRequestSchema.index({ bankId: 1, status: 1 });

module.exports = mongoose.model('BloodRequest', bloodRequestSchema);
