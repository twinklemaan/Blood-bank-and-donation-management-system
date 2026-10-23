// events.js - Event Driven Architecture with Node.js EventEmitter (Unit 3: Events & File System)
const EventEmitter = require('events');
const fs = require('fs');
const path = require('path');
const { sendNotificationEmail } = require('./mailer');

class BloodBankEmitter extends EventEmitter {}

const appEvents = new BloodBankEmitter();
const logFilePath = path.join(__dirname, '../../logs.txt');

// Listener (a): Audit logging using core 'fs' module appendFile
appEvents.on('requestFulfilled', (data) => {
  const timestamp = new Date().toISOString();
  const outcome = data.outcome || data.status;
  const requestId = data.request ? data.request._id : 'N/A';
  const units = data.request ? data.request.unitsRequested : 'N/A';
  const bloodGroup = data.request ? data.request.bloodGroup : 'N/A';

  const logEntry = `[${timestamp}] EVENT requestFulfilled | RequestID: ${requestId} | BloodGroup: ${bloodGroup} | Units: ${units} | Outcome: ${outcome}\n`;

  fs.appendFile(logFilePath, logEntry, 'utf8', (err) => {
    if (err) {
      console.error('Failed to append event audit log:', err.message);
    }
  });
});

// Listener (b): Hospital email dispatch via mailer.js
appEvents.on('requestFulfilled', async (data) => {
  const outcome = data.outcome || data.status;
  const request = data.request;
  const recipient = (request && request.hospitalEmail) || 'hospital@bloodbank.org';

  await sendNotificationEmail({
    to: recipient,
    subject: `Blood Request Status: ${outcome}`,
    text: `Your blood request for ${request ? request.unitsRequested : ''} unit(s) of ${request ? request.bloodGroup : ''} has been marked as ${outcome}.`,
  });
});

module.exports = appEvents;
