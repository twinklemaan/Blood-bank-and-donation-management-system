// test-phase3.js - Live automated test suite for Phase 3 API endpoints
require('dotenv').config();
const http = require('http');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const app = require('./src/app');
const User = require('./src/models/User');
const Bank = require('./src/models/Bank');
const Donor = require('./src/models/Donor');
const Donation = require('./src/models/Donation');

// Helper to make HTTP requests using Node core http module
function request({ method, path, headers = {}, body = null, port }) {
  return new Promise((resolve, reject) => {
    const dataString = body ? JSON.stringify(body) : null;
    const reqHeaders = { ...headers };
    if (dataString) {
      reqHeaders['Content-Type'] = 'application/json';
      reqHeaders['Content-Length'] = Buffer.byteLength(dataString);
    }

    const req = http.request(
      {
        hostname: '127.0.0.1',
        port,
        path,
        method,
        headers: reqHeaders,
      },
      (res) => {
        let responseBody = '';
        res.on('data', (chunk) => (responseBody += chunk));
        res.on('end', () => {
          let parsed;
          try {
            parsed = JSON.parse(responseBody);
          } catch {
            parsed = responseBody;
          }
          resolve({ status: res.statusCode, body: parsed });
        });
      }
    );

    req.on('error', reject);
    if (dataString) req.write(dataString);
    req.end();
  });
}

async function runTests() {
  console.log('--- Starting Phase 3 Live API Tests ---');

  // 1. Connect to MongoDB
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to MongoDB for testing.');

  // Clean test data
  await User.deleteMany({ username: { $in: ['test_staff', 'test_hosp'] } });
  await Bank.deleteMany({ name: 'Pune Central Blood Bank' });
  await Donor.deleteMany({ name: 'Test Donor Rahul' });

  // 2. Provision test Bank
  const bank = await Bank.create({
    name: 'Pune Central Blood Bank',
    city: 'Pune',
    phone: '020-25678901',
  });

  // Provision test Staff user
  const salt = await bcrypt.genSalt(10);
  const passwordHash = await bcrypt.hash('staff123', salt);
  await User.create({
    username: 'test_staff',
    passwordHash,
    role: 'STAFF',
    name: 'Dr. Ramesh Staff',
    city: 'Pune',
    bankId: bank._id,
  });

  // Start test server on ephemeral port 5055
  const TEST_PORT = 5055;
  const server = http.createServer(app);
  await new Promise((resolve) => server.listen(TEST_PORT, resolve));
  console.log(`Test server running on port ${TEST_PORT}`);

  try {
    // 3. Test Health Check
    const healthRes = await request({ method: 'GET', path: '/api/health', port: TEST_PORT });
    console.log('✔ GET /api/health ->', healthRes.status, healthRes.body.status);
    if (healthRes.status !== 200) throw new Error('Health check failed');

    // 4. Test Hospital Registration
    const regRes = await request({
      method: 'POST',
      path: '/api/auth/register',
      port: TEST_PORT,
      body: {
        username: 'test_hosp',
        password: 'hospitalPass123',
        name: 'Apollo Hospital Pune',
        city: 'Pune',
      },
    });
    console.log('✔ POST /api/auth/register ->', regRes.status, regRes.body.message);
    if (regRes.status !== 201) throw new Error('Registration failed');

    // 5. Test Staff Login
    const staffLoginRes = await request({
      method: 'POST',
      path: '/api/auth/login',
      port: TEST_PORT,
      body: { username: 'test_staff', password: 'staff123' },
    });
    console.log('✔ POST /api/auth/login (Staff) ->', staffLoginRes.status, 'Token acquired');
    const staffToken = staffLoginRes.body.token;

    // 6. Test Hospital Login
    const hospLoginRes = await request({
      method: 'POST',
      path: '/api/auth/login',
      port: TEST_PORT,
      body: { username: 'test_hosp', password: 'hospitalPass123' },
    });
    console.log('✔ POST /api/auth/login (Hospital) ->', hospLoginRes.status, 'Token acquired');
    const hospToken = hospLoginRes.body.token;

    // 7. Test Negative Auth Check: Hospital trying to access Staff route
    const hospForbiddenRes = await request({
      method: 'GET',
      path: '/api/donors',
      headers: { Authorization: `Bearer ${hospToken}` },
      port: TEST_PORT,
    });
    console.log('✔ Negative test: Hospital calling Staff route ->', hospForbiddenRes.status, hospForbiddenRes.body.message);
    if (hospForbiddenRes.status !== 403) throw new Error('Role check failed for hospital calling staff route');

    // 8. Test Donor Creation (STAFF)
    const createDonorRes = await request({
      method: 'POST',
      path: '/api/donors',
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
      body: {
        name: 'Test Donor Rahul',
        gender: 'Male',
        dob: '1995-05-15',
        bloodGroup: 'O+',
        phones: ['9876543210', '9876543211'],
        address: { city: 'Pune', pincode: '411001' },
      },
    });
    console.log('✔ POST /api/donors ->', createDonorRes.status, 'Donor ID:', createDonorRes.body._id);
    const donorId = createDonorRes.body._id;

    // 9. Test Eligibility Check
    const eligRes = await request({
      method: 'GET',
      path: `/api/donors/${donorId}/eligibility`,
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
    });
    console.log('✔ GET /api/donors/:id/eligibility ->', eligRes.status, eligRes.body);

    // 10. Test Donation Intake (STAFF)
    const donationRes = await request({
      method: 'POST',
      path: '/api/donations',
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
      body: {
        donorId,
        units: 2,
        hemoglobin: 14.5,
      },
    });
    console.log('✔ POST /api/donations ->', donationRes.status, 'Donation ID:', donationRes.body._id, 'Status:', donationRes.body.screeningResult);
    const donationId = donationRes.body._id;

    // 11. Test Negative Rule: 90 days gap rejection
    const immediateDonationRes = await request({
      method: 'POST',
      path: '/api/donations',
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
      body: {
        donorId,
        units: 1,
        hemoglobin: 14.0,
      },
    });
    console.log('✔ Negative test: 90-day donation interval ->', immediateDonationRes.status, immediateDonationRes.body.message);
    if (immediateDonationRes.status !== 400) throw new Error('90-day interval not enforced');

    // 12. Test Negative Rule: Delete donor with donation history
    const deleteRes = await request({
      method: 'DELETE',
      path: `/api/donors/${donorId}`,
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
    });
    console.log('✔ Negative test: Delete donor with history ->', deleteRes.status, deleteRes.body.message);
    if (deleteRes.status !== 400) throw new Error('Donor deletion restriction failed');

    // 13. Test Screening Transition: Pending -> Pass
    const screenRes = await request({
      method: 'PATCH',
      path: `/api/donations/${donationId}/screening`,
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
      body: { result: 'Pass' },
    });
    console.log('✔ PATCH /api/donations/:id/screening ->', screenRes.status, 'New Result:', screenRes.body.screeningResult);

    // 14. Test Negative Rule: Final screening state cannot change
    const reScreenRes = await request({
      method: 'PATCH',
      path: `/api/donations/${donationId}/screening`,
      headers: { Authorization: `Bearer ${staffToken}` },
      port: TEST_PORT,
      body: { result: 'Fail' },
    });
    console.log('✔ Negative test: Cannot re-screen finalized donation ->', reScreenRes.status, reScreenRes.body.message);
    if (reScreenRes.status !== 400) throw new Error('Screening immutability not enforced');

    console.log('\n--- ALL PHASE 3 API TESTS PASSED SUCCESSFULLY ---');
  } finally {
    server.close();
    await mongoose.disconnect();
  }
}

runTests().catch((err) => {
  console.error('Test Failed:', err);
  process.exit(1);
});
