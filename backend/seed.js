// seed.js - Database Seeding Script (Unit 4: MongoDB Atlas / Local Provisioning)
require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const User = require('./src/models/User');
const Bank = require('./src/models/Bank');
const Donor = require('./src/models/Donor');
const Donation = require('./src/models/Donation');
const BloodRequest = require('./src/models/BloodRequest');
const rules = require('./src/rules');

async function seed() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error('ERROR: MONGODB_URI is required to run seed script.');
    process.exit(1);
  }

  console.log('Connecting to MongoDB database for seeding...');
  await mongoose.connect(uri);

  console.log('Clearing existing database collections...');
  await Promise.all([
    User.deleteMany({}),
    Bank.deleteMany({}),
    Donor.deleteMany({}),
    Donation.deleteMany({}),
    BloodRequest.deleteMany({}),
  ]);

  console.log('1. Seeding Blood Banks...');
  const bank1 = await Bank.create({
    name: 'Pune Central Blood Bank',
    city: 'Pune',
    phone: '020-25678901',
  });

  const bank2 = await Bank.create({
    name: 'Mumbai Lifeline Blood Centre',
    city: 'Mumbai',
    phone: '022-24123456',
  });

  console.log('2. Seeding Users (Passwords hashed with bcryptjs)...');
  const staffHash = await bcrypt.hash('staff123', 10);
  const hospHash = await bcrypt.hash('hosp123', 10);

  const staff1 = await User.create({
    username: 'staff1',
    passwordHash: staffHash,
    role: 'STAFF',
    name: 'Dr. Suresh Staff (Pune)',
    city: 'Pune',
    bankId: bank1._id,
  });

  const staff2 = await User.create({
    username: 'staff2',
    passwordHash: staffHash,
    role: 'STAFF',
    name: 'Dr. Meera Staff (Mumbai)',
    city: 'Mumbai',
    bankId: bank2._id,
  });

  const hosp1 = await User.create({
    username: 'hosp1',
    passwordHash: hospHash,
    role: 'HOSPITAL',
    name: 'Ruby Hall Clinic',
    city: 'Pune',
  });

  console.log('3. Seeding 6 Donors...');
  const donorsData = [
    {
      name: 'Amit Verma',
      gender: 'Male',
      dob: '1995-04-12',
      bloodGroup: 'O+',
      phones: ['9822011111', '9822011112'],
      address: { city: 'Pune', pincode: '411001' },
    },
    {
      name: 'Priya Sharma',
      gender: 'Female',
      dob: '1998-08-20',
      bloodGroup: 'A+',
      phones: ['9822022222'],
      address: { city: 'Pune', pincode: '411004' },
    },
    {
      name: 'Rohan Patil',
      gender: 'Male',
      dob: '1992-11-05',
      bloodGroup: 'B+',
      phones: ['9822033333'],
      address: { city: 'Pune', pincode: '411014' },
    },
    {
      name: 'Neha Kulkarni',
      gender: 'Female',
      dob: '2000-02-14',
      bloodGroup: 'AB+',
      phones: ['9822044444'],
      address: { city: 'Mumbai', pincode: '400001' },
    },
    {
      name: 'Vikram Joshi',
      gender: 'Male',
      dob: '1988-06-30',
      bloodGroup: 'O-',
      phones: ['9822055555'],
      address: { city: 'Mumbai', pincode: '400012' },
    },
    {
      name: 'Anjali Deshmukh',
      gender: 'Female',
      dob: '1996-09-18',
      bloodGroup: 'A-',
      phones: ['9822066666'],
      address: { city: 'Pune', pincode: '411038' },
    },
  ];

  const createdDonors = [];
  for (const d of donorsData) {
    rules.checkDonorAge(d.dob, new Date());
    const donor = await Donor.create(d);
    createdDonors.push(donor);
  }

  console.log('4. Seeding 8 Donations (through business rule validation)...');
  const now = new Date();
  const pastDate5Days = new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000);
  const pastDate12Days = new Date(now.getTime() - 12 * 24 * 60 * 60 * 1000);

  const donationsToCreate = [
    // Pune Bank Donations (Pass stock)
    { donorIndex: 0, bankId: bank1._id, date: pastDate12Days, units: 2, hb: 14.2, screening: 'Pass' },
    { donorIndex: 1, bankId: bank1._id, date: pastDate5Days, units: 3, hb: 13.5, screening: 'Pass' },
    { donorIndex: 2, bankId: bank1._id, date: now, units: 4, hb: 14.8, screening: 'Pass' },
    { donorIndex: 5, bankId: bank1._id, date: now, units: 1, hb: 12.8, screening: 'Pending' },

    // Mumbai Bank Donations
    { donorIndex: 3, bankId: bank2._id, date: pastDate12Days, units: 2, hb: 13.0, screening: 'Pass' },
    { donorIndex: 4, bankId: bank2._id, date: pastDate5Days, units: 2, hb: 15.1, screening: 'Pass' },
    { donorIndex: 3, bankId: bank2._id, date: now, units: 1, hb: 12.0, screening: 'Fail' }, // Screened as Fail
    { donorIndex: 4, bankId: bank2._id, date: now, units: 2, hb: 14.4, screening: 'Pass' },
  ];

  for (const item of donationsToCreate) {
    const donor = createdDonors[item.donorIndex];
    rules.checkDonorAge(donor.dob, item.date);
    const expiry = rules.calculateExpiryDate(item.date);

    await Donation.create({
      donorId: donor._id,
      bankId: item.bankId,
      bloodGroup: donor.bloodGroup,
      donationDate: item.date,
      expiryDate: expiry,
      units: item.units,
      unitsRemaining: item.screening === 'Pass' ? item.units : 0,
      hemoglobin: item.hb,
      screeningResult: item.screening,
    });
  }

  console.log('5. Seeding 2 Blood Requests from Hospital...');
  await BloodRequest.create({
    hospitalId: hosp1._id,
    bankId: bank1._id,
    bloodGroup: 'O+',
    unitsRequested: 1,
    urgency: 'Critical',
    status: 'Pending',
    requestDate: new Date(now.getTime() - 2 * 60 * 60 * 1000), // 2 hrs ago
    issues: [],
  });

  await BloodRequest.create({
    hospitalId: hosp1._id,
    bankId: bank1._id,
    bloodGroup: 'A+',
    unitsRequested: 2,
    urgency: 'Normal',
    status: 'Pending',
    requestDate: new Date(now.getTime() - 1 * 60 * 60 * 1000), // 1 hr ago
    issues: [],
  });

  console.log('\n================ SEED SUMMARY ================');
  console.log('Banks created: 2 (Pune, Mumbai)');
  console.log('Logins provisioned:');
  console.log('  Staff Pune:   username = staff1, password = staff123');
  console.log('  Staff Mumbai: username = staff2, password = staff123');
  console.log('  Hospital:     username = hosp1,  password = hosp123');
  console.log('Donors: 6');
  console.log('Donations: 8 (Pass, Pending, Fail)');
  console.log('Requests: 2 Pending requests');
  console.log('==============================================');

  await mongoose.disconnect();
  console.log('Seed completed successfully.');
}

seed().catch((err) => {
  console.error('Seed error:', err);
  process.exit(1);
});
