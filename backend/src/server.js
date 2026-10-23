// server.js - Application Entry Point (Unit 3: Core HTTP Module & Server Startup)
require('dotenv').config();
const http = require('http'); // Node.js core HTTP module from syllabus Unit 3
const app = require('./app');
const connectDB = require('./db');

// Validation: Ensure mandatory environment variables are present before starting
if (!process.env.MONGODB_URI) {
  console.error('FATAL ERROR: MONGODB_URI is not defined in environment variables.');
  process.exit(1);
}

if (!process.env.JWT_SECRET) {
  console.error('FATAL ERROR: JWT_SECRET is not defined in environment variables.');
  process.exit(1);
}

const PORT = process.env.PORT || 5000;

// SYLLABUS CONCEPT (Unit 3): HTTP Module
// We explicitly use http.createServer(app) instead of app.listen() to demonstrate
// the integration of Express with Node's low-level core HTTP server module.
// This allows attaching WebSockets, custom protocol upgrades, or SSL certificates directly.
const server = http.createServer(app);

// Connect to MongoDB and then start listening for requests
connectDB().then(() => {
  server.listen(PORT, () => {
    console.log(`Blood Bank API Server running on port ${PORT}`);
    console.log(`Health Check: http://localhost:${PORT}/api/health`);
  });
});
