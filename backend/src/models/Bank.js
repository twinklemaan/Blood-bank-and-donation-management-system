// Bank.js - Mongoose Model for Blood Banks (Unit 4: Data modelling)
const mongoose = require('mongoose');

const bankSchema = new mongoose.Schema(
  {
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
    phone: {
      type: String,
      required: true,
      trim: true,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Bank', bankSchema);
