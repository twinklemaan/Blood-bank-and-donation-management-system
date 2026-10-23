// test-phase5.js - Verification of Node.js concepts: HTTP demo, file upload, fs.unlink, mailer
require('dotenv').config();
const http = require('http');
const fs = require('fs');
const path = require('path');
const mongoose = require('mongoose');
const app = require('./src/app');
const Donation = require('./src/models/Donation');
const User = require('./src/models/User');

function request({ method, path, headers = {}, body = null, port }) {
  return new Promise((resolve, reject) => {
    const dataString = body && !(body instanceof Buffer) ? JSON.stringify(body) : body;
    const reqHeaders = { ...headers };
    if (dataString && !(body instanceof Buffer)) {
      reqHeaders['Content-Type'] = 'application/json';
      reqHeaders['Content-Length'] = Buffer.byteLength(dataString);
    } else if (body instanceof Buffer) {
      reqHeaders['Content-Length'] = body.length;
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

// Helper to construct multipart/form-data for file upload test
function makeMultipartBody(fieldName, filename, mimeType, fileContent) {
  const boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW';
  const prefix = `--${boundary}\r\nContent-Disposition: form-data; name="${fieldName}"; filename="${filename}"\r\nContent-Type: ${mimeType}\r\n\r\n`;
  const suffix = `\r\n--${boundary}--\r\n`;
  const body = Buffer.concat([
    Buffer.from(prefix, 'utf8'),
    Buffer.from(fileContent, 'utf8'),
    Buffer.from(suffix, 'utf8'),
  ]);
  return {
    boundary,
    body,
  };
}

async function runTests() {
  console.log('--- Testing Phase 5: Node.js Concepts & File Upload ---');

  // 1. Test HTTP Demo Server (Unit 3)
  console.log('1. Testing demos/http-demo.js...');
  const httpDemoServer = require('./demos/http-demo');
  const demoResRoot = await request({ method: 'GET', path: '/', port: 3000 });
  console.log('✔ GET http://localhost:3000/ ->', demoResRoot.status, demoResRoot.body);

  const demoResStock = await request({ method: 'GET', path: '/stock?group=O%2B', port: 3000 });
  console.log('✔ GET http://localhost:3000/stock?group=O%2B ->', demoResStock.status, demoResStock.body);
  httpDemoServer.close();

  // 2. Connect DB and start app server
  await mongoose.connect(process.env.MONGODB_URI);
  const TEST_PORT = 5057;
  const apiServer = http.createServer(app);
  await new Promise((resolve) => apiServer.listen(TEST_PORT, resolve));

  try {
    // Login as staff1
    const loginRes = await request({
      method: 'POST',
      path: '/api/auth/login',
      port: TEST_PORT,
      body: { username: 'staff1', password: 'staff123' },
    });
    const token = loginRes.body.token;

    // Fetch a donation belonging to staff1's bank
    const staffUser = await User.findOne({ username: 'staff1' });
    const donation = await Donation.findOne({ bankId: staffUser.bankId });

    // 3. Upload first report file
    console.log('2. Testing Multer file upload: POST /api/donations/:id/report...');
    const upload1 = makeMultipartBody('report', 'lab_report_1.pdf', 'application/pdf', '%PDF-1.4 test sample content 1');
    const uploadRes1 = await request({
      method: 'POST',
      path: `/api/donations/${donation._id}/report`,
      port: TEST_PORT,
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': `multipart/form-data; boundary=${upload1.boundary}`,
      },
      body: upload1.body,
    });
    console.log('✔ Upload 1 response ->', uploadRes1.status, uploadRes1.body);
    const firstFilename = uploadRes1.body.reportFile;
    const firstFilePath = path.join(__dirname, 'uploads', firstFilename);
    if (!fs.existsSync(firstFilePath)) throw new Error('Uploaded file was not found on disk');

    // 4. Upload replacement file (tests fs.unlink)
    console.log('3. Testing file replacement and fs.unlink...');
    const upload2 = makeMultipartBody('report', 'lab_report_2.png', 'image/png', 'PNG test content');
    const uploadRes2 = await request({
      method: 'POST',
      path: `/api/donations/${donation._id}/report`,
      port: TEST_PORT,
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': `multipart/form-data; boundary=${upload2.boundary}`,
      },
      body: upload2.body,
    });
    console.log('✔ Upload 2 response ->', uploadRes2.status, uploadRes2.body);
    const secondFilename = uploadRes2.body.reportFile;
    const secondFilePath = path.join(__dirname, 'uploads', secondFilename);

    // Verify first file was unlinked and second file exists
    if (fs.existsSync(firstFilePath)) {
      throw new Error('Old file was not unlinked on replacement');
    }
    if (!fs.existsSync(secondFilePath)) {
      throw new Error('Second replacement file does not exist on disk');
    }
    console.log('✔ fs.unlink successfully removed old file upon replacement.');

    // Clean up test upload file
    fs.unlinkSync(secondFilePath);

    // 5. Test Mailer non-blocking failure
    console.log('4. Testing mailer fallback...');
    const { sendNotificationEmail } = require('./src/mailer');
    const mailResult = await sendNotificationEmail({
      to: 'hospital@test.com',
      subject: 'Test Subject',
      text: 'Test Body',
    });
    console.log('✔ Mailer fallback handled gracefully (result:', mailResult, ')');

    console.log('\n--- ALL PHASE 5 TESTS PASSED SUCCESSFULLY ---');
  } finally {
    apiServer.close();
    await mongoose.disconnect();
  }
}

runTests().catch((err) => {
  console.error('Phase 5 Test Error:', err);
  process.exit(1);
});
