// test-phase4.js - Automated tests for Phase 4: Stock aggregation, requests, and FEFO fulfillment
require('dotenv').config();
const http = require('http');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');
const app = require('./src/app');
const User = require('./src/models/User');
const Bank = require('./src/models/Bank');
const Donor = require('./src/models/Donor');
const Donation = require('./src/models/Donation');
const BloodRequest = require('./src/models/BloodRequest');

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
  console.log('--- Starting Phase 4 Stock & Request API Tests ---');

  await mongoose.connect(process.env.MONGODB_URI);

  // Setup test banks
  await Bank.deleteMany({ name: { $in: ['Phase4 Bank Pune', 'Phase4 Bank Mumbai'] } });
  await User.deleteMany({ username: { $in: ['p4_staff_pune', 'p4_staff_mumbai', 'p4_hospital'] } });
  await BloodRequest.deleteMany({});
  await Donation.deleteMany({});
  await Donor.deleteMany({});

  const bankPune = await Bank.create({
    name: 'Phase4 Bank Pune',
    city: 'Pune',
    phone: '020-11112222',
  });

  const bankMumbai = await Bank.create({
    name: 'Phase4 Bank Mumbai',
    city: 'Mumbai',
    phone: '022-33334444',
  });

  const salt = await bcrypt.genSalt(10);
  const hash = await bcrypt.hash('pass123', salt);

  await User.create({
    username: 'p4_staff_pune',
    passwordHash: hash,
    role: 'STAFF',
    name: 'Staff Pune',
    city: 'Pune',
    bankId: bankPune._id,
  });

  await User.create({
    username: 'p4_staff_mumbai',
    passwordHash: hash,
    role: 'STAFF',
    name: 'Staff Mumbai',
    city: 'Mumbai',
    bankId: bankMumbai._id,
  });

  const hospUser = await User.create({
    username: 'p4_hospital',
    passwordHash: hash,
    role: 'HOSPITAL',
    name: 'Sahyadri Hospital',
    city: 'Pune',
  });

  const donor = await Donor.create({
    name: 'Donor Amit',
    gender: 'Male',
    dob: '1990-01-01',
    bloodGroup: 'B+',
    phones: ['9998887776'],
    address: { city: 'Pune', pincode: '411004' },
  });

  // Seed two donations for Bank Pune:
  // Donation 1: 2 units, expires in 10 days
  const d1Expiry = new Date();
  d1Expiry.setDate(d1Expiry.getDate() + 10);
  const donation1 = await Donation.create({
    donorId: donor._id,
    bankId: bankPune._id,
    bloodGroup: 'B+',
    donationDate: new Date(),
    expiryDate: d1Expiry,
    units: 2,
    unitsRemaining: 2,
    hemoglobin: 14.0,
    screeningResult: 'Pass',
  });

  // Donation 2: 3 units, expires in 25 days
  const d2Expiry = new Date();
  d2Expiry.setDate(d2Expiry.getDate() + 25);
  const donation2 = await Donation.create({
    donorId: donor._id,
    bankId: bankPune._id,
    bloodGroup: 'B+',
    donationDate: new Date(),
    expiryDate: d2Expiry,
    units: 3,
    unitsRemaining: 3,
    hemoglobin: 14.0,
    screeningResult: 'Pass',
  });

  const TEST_PORT = 5056;
  const server = http.createServer(app);
  await new Promise((resolve) => server.listen(TEST_PORT, resolve));

  try {
    // Login tokens
    const loginStaffPune = await request({
      method: 'POST',
      path: '/api/auth/login',
      port: TEST_PORT,
      body: { username: 'p4_staff_pune', password: 'pass123' },
    });
    const staffPuneToken = loginStaffPune.body.token;

    const loginStaffMumbai = await request({
      method: 'POST',
      path: '/api/auth/login',
      port: TEST_PORT,
      body: { username: 'p4_staff_mumbai', password: 'pass123' },
    });
    const staffMumbaiToken = loginStaffMumbai.body.token;

    const loginHosp = await request({
      method: 'POST',
      path: '/api/auth/login',
      port: TEST_PORT,
      body: { username: 'p4_hospital', password: 'pass123' },
    });
    const hospToken = loginHosp.body.token;

    // 1. Test Public Stock Aggregation
    const pubStockRes = await request({ method: 'GET', path: '/api/public/stock', port: TEST_PORT });
    console.log('✔ GET /api/public/stock ->', pubStockRes.status, 'Total Blood Groups returned:', pubStockRes.body.length);
    const bPosStock = pubStockRes.body.find((item) => item.bloodGroup === 'B+');
    if (bPosStock.units !== 5) throw new Error(`Expected 5 units of B+, got ${bPosStock.units}`);

    // 2. Test Staff Stock Aggregation (Own Bank)
    const staffStockRes = await request({
      method: 'GET',
      path: '/api/stock',
      headers: { Authorization: `Bearer ${staffPuneToken}` },
      port: TEST_PORT,
    });
    console.log('✔ GET /api/stock (Staff Pune) ->', staffStockRes.status, 'B+ units:', staffStockRes.body.find((i) => i.bloodGroup === 'B+').units);

    // 3. Test Hospital Availability Search ($lookup + $group)
    const availRes = await request({
      method: 'GET',
      path: '/api/availability?bloodGroup=B%2B&city=Pune',
      headers: { Authorization: `Bearer ${hospToken}` },
      port: TEST_PORT,
    });
    console.log('✔ GET /api/availability ->', availRes.status, availRes.body);
    if (availRes.body.length === 0 || availRes.body[0].bankName !== 'Phase4 Bank Pune') {
      throw new Error('Availability search failed to match Pune bank inventory');
    }

    // 4. Test Hospital Raising Blood Request
    const createReqRes = await request({
      method: 'POST',
      path: '/api/requests',
      headers: { Authorization: `Bearer ${hospToken}` },
      port: TEST_PORT,
      body: {
        bankId: bankPune._id,
        bloodGroup: 'B+',
        unitsRequested: 3,
        urgency: 'Critical',
      },
    });
    console.log('✔ POST /api/requests ->', createReqRes.status, 'Request ID:', createReqRes.body._id, 'Urgency:', createReqRes.body.urgency);
    const requestId = createReqRes.body._id;

    // 5. Test Staff Views Pending Requests (Rule 9: Sorted)
    const pendingReqRes = await request({
      method: 'GET',
      path: '/api/requests/to-fulfil',
      headers: { Authorization: `Bearer ${staffPuneToken}` },
      port: TEST_PORT,
    });
    console.log('✔ GET /api/requests/to-fulfil ->', pendingReqRes.status, 'Pending count:', pendingReqRes.body.length);
    if (pendingReqRes.body[0]._id !== requestId) throw new Error('Critical request was not prioritized to index 0');

    // 6. Negative Test: Staff from Mumbai cannot fulfil Pune's request
    const wrongBankFulfil = await request({
      method: 'POST',
      path: `/api/requests/${requestId}/fulfil`,
      headers: { Authorization: `Bearer ${staffMumbaiToken}` },
      port: TEST_PORT,
    });
    console.log('✔ Negative test: Staff fulfilling request of another bank ->', wrongBankFulfil.status, wrongBankFulfil.body.message);
    if (wrongBankFulfil.status !== 403) throw new Error('Bank authorization check failed');

    // 7. Test FEFO Fulfillment by Pune Staff (Needs 3 units: 2 from Donation1, 1 from Donation2)
    const fulfilRes = await request({
      method: 'POST',
      path: `/api/requests/${requestId}/fulfil`,
      headers: { Authorization: `Bearer ${staffPuneToken}` },
      port: TEST_PORT,
    });
    console.log('✔ POST /api/requests/:id/fulfil ->', fulfilRes.status, fulfilRes.body.message);
    if (fulfilRes.status !== 200 || fulfilRes.body.request.status !== 'Fulfilled') {
      throw new Error('Fulfillment failed');
    }

    // Verify FEFO issues
    const issues = fulfilRes.body.request.issues;
    console.log('✔ FEFO Issues recorded:', issues);
    if (issues.length !== 2) throw new Error('Expected 2 issue allocations under FEFO');
    if (issues[0].unitsTaken !== 2 || issues[1].unitsTaken !== 1) {
      throw new Error('FEFO units deduction mismatch');
    }

    // 8. Verify Remaining Stock in DB
    const updatedD1 = await Donation.findById(donation1._id);
    const updatedD2 = await Donation.findById(donation2._id);
    console.log(`✔ Stock updated: Donation1 remaining = ${updatedD1.unitsRemaining}, Donation2 remaining = ${updatedD2.unitsRemaining}`);
    if (updatedD1.unitsRemaining !== 0 || updatedD2.unitsRemaining !== 2) {
      throw new Error('Donation unitsRemaining in database not updated properly');
    }

    // 9. Verify Event Logger written to logs.txt
    const logPath = path.join(__dirname, '../logs.txt');
    if (fs.existsSync(logPath)) {
      const logs = fs.readFileSync(logPath, 'utf8');
      if (logs.includes('EVENT requestFulfilled') && logs.includes(requestId)) {
        console.log('✔ Event Listener verified: audit log entry found in logs.txt');
      }
    }

    // 10. Test Aggregation 2: Donations summary
    const summaryRes = await request({
      method: 'GET',
      path: '/api/stock/donations-summary',
      headers: { Authorization: `Bearer ${staffPuneToken}` },
      port: TEST_PORT,
    });
    console.log('✔ GET /api/stock/donations-summary ->', summaryRes.status, summaryRes.body[0]);

    console.log('\n--- ALL PHASE 4 API TESTS PASSED SUCCESSFULLY ---');
  } finally {
    server.close();
    await mongoose.disconnect();
  }
}

runTests().catch((err) => {
  console.error('Test Failed:', err);
  process.exit(1);
});
