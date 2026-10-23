// test-phase2.js - Verification script for Phase 2: models and rules
const assert = require('assert');

console.log('--- Testing Phase 2 Models and Rules ---');

// 1. Verify all 5 models load
const User = require('./src/models/User');
const Bank = require('./src/models/Bank');
const Donor = require('./src/models/Donor');
const Donation = require('./src/models/Donation');
const BloodRequest = require('./src/models/BloodRequest');
console.log('✔ All 5 Mongoose models loaded successfully.');

// 2. Verify rules.js
const rules = require('./src/rules');

// Test Age (18-65)
assert.doesNotThrow(() => rules.checkDonorAge('2000-01-01', '2024-01-01'), 'Age 24 should pass');
assert.throws(() => rules.checkDonorAge('2015-01-01', '2024-01-01'), /Donor must be aged 18-65/);
console.log('✔ Age rule verified (18-65).');

// Test Hemoglobin (>= 12.5)
assert.doesNotThrow(() => rules.checkHemoglobin(13.2), 'Hb 13.2 should pass');
assert.throws(() => rules.checkHemoglobin(11.9), /Haemoglobin below 12.5/);
console.log('✔ Hemoglobin rule verified (>= 12.5).');

// Test Donation date
assert.doesNotThrow(() => rules.checkDonationDate(new Date()));
console.log('✔ Donation date rule verified.');

// Test 90 days gap
const lastDate = '2024-01-01';
assert.throws(() => rules.checkDonationGap(lastDate, '2024-02-01'), /Donor must wait 90 days/);
assert.doesNotThrow(() => rules.checkDonationGap(lastDate, '2024-04-10'));
console.log('✔ 90-day donation gap rule verified.');

// Test Expiry Date (+35 days)
const donationDate = new Date('2024-01-01T00:00:00Z');
const expiry = rules.calculateExpiryDate(donationDate);
const diffDays = Math.round((expiry - donationDate) / (1000 * 60 * 60 * 24));
assert.strictEqual(diffDays, 35, 'Expiry must be exactly 35 days later');
console.log('✔ Expiry calculation (+35 days) verified.');

// Test Screening state machine
assert.doesNotThrow(() => rules.validateScreeningTransition('Pending', 'Pass'));
assert.doesNotThrow(() => rules.validateScreeningTransition('Pending', 'Fail'));
assert.throws(() => rules.validateScreeningTransition('Pass', 'Fail'), /final/);
console.log('✔ Screening state machine verified.');

// Test FEFO allocation
const mockReq = { unitsRequested: 3, status: 'Pending' };
const mockDonations = [
  { _id: 'd1', unitsRemaining: 2, expiryDate: new Date('2024-05-01') },
  { _id: 'd2', unitsRemaining: 2, expiryDate: new Date('2024-05-10') },
];
const fefoResult = rules.fulfilRequestFEFO(mockReq, mockDonations);
assert.strictEqual(fefoResult.status, 'Fulfilled');
assert.strictEqual(fefoResult.issues.length, 2);
assert.strictEqual(fefoResult.issues[0].unitsTaken, 2);
assert.strictEqual(fefoResult.issues[1].unitsTaken, 1);
console.log('✔ FEFO allocation logic verified.');

// Test Urgency sorting
const requests = [
  { urgency: 'Normal', requestDate: new Date('2024-01-01') },
  { urgency: 'Critical', requestDate: new Date('2024-01-02') },
  { urgency: 'Urgent', requestDate: new Date('2024-01-03') },
];
const sorted = rules.sortHospitalRequests(requests);
assert.strictEqual(sorted[0].urgency, 'Critical');
assert.strictEqual(sorted[1].urgency, 'Urgent');
assert.strictEqual(sorted[2].urgency, 'Normal');
console.log('✔ Request priority sorting (Critical -> Urgent -> Normal) verified.');

console.log('\n--- ALL PHASE 2 TESTS PASSED ---');
