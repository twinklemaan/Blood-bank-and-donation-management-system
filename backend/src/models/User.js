// User.js - Mongoose Model for Application Users (Unit 4 & 6: Data modelling, Auth)
const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    // Index 1 of 3: Unique index on username for quick lookup and preventing duplicates
    username: {
      type: String,
      required: true,
      unique: true,
      trim: true,
    },
    passwordHash: {
      type: String,
      required: true,
    },
    role: {
      type: String,
      required: true,
      enum: ['STAFF', 'HOSPITAL'],
    },
    name: {
      type: String,
      required: true,
      trim: true,
    },
    city: {
      type: String,
      required: true,
      trim: true,
    },
    // Only populated when role is 'STAFF' to bind staff to their specific blood bank
    bankId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Bank',
      default: null,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('User', userSchema);
