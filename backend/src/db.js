// db.js - MongoDB connection setup using Mongoose (Unit 4: Database connectivity)
const mongoose = require('mongoose');

async function connectDB() {
  const uri = process.env.MONGODB_URI;

  // The server must print a clear message and exit if MONGODB_URI is missing
  if (!uri) {
    console.error('CRITICAL CONFIGURATION ERROR: MONGODB_URI is not set in environment variables.');
    console.error('Please define MONGODB_URI in backend/.env to connect to your MongoDB Atlas cluster.');
    process.exit(1);
  }

  try {
    await mongoose.connect(uri);
    console.log('Successfully connected to MongoDB Atlas database.');
  } catch (error) {
    console.error('MongoDB connection error:', error.message);
    process.exit(1);
  }
}

module.exports = connectDB;
