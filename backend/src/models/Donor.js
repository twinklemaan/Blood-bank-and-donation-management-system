// Donor.js - Mongoose Model for Blood Donors (Unit 4: Data modelling)
const mongoose = require('mongoose');

const donorSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },
    gender: {
      type: String,
      required: true,
      enum: ['Male', 'Female', 'Other'],
    },
    dob: {
      type: Date,
      required: true,
    },
    bloodGroup: {
      type: String,
      required: true,
      enum: ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'],
    },
    // DATA MODELLING CONCEPT: One-to-Many (Embedded)
    // A single donor can possess multiple phone numbers stored directly in an array.
    phones: {
      type: [String],
      required: true,
      validate: {
        validator: (v) => Array.isArray(v) && v.length > 0,
        message: 'At least one contact phone number is required',
      },
    },
    // DATA MODELLING CONCEPT: One-to-One (Embedded)
    // A donor has exactly one address object embedded inside the document.
    address: {
      city: {
        type: String,
        required: true,
        trim: true,
      },
      pincode: {
        type: String,
        required: true,
        trim: true,
        match: [/^\d{6}$/, 'Pincode must be exactly 6 digits'],
      },
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Donor', donorSchema);
