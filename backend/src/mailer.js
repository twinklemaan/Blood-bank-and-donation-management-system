// mailer.js - Email Notification Service (Unit 3: Core Node.js Modules & Nodemailer)
const nodemailer = require('nodemailer');

/**
 * Sends an email notification to the hospital when their request status updates.
 * If SMTP configuration is missing or network fails, logs gracefully and continues.
 */
async function sendNotificationEmail({ to, subject, text, html }) {
  const host = process.env.SMTP_HOST;
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASS;

  if (!host || !user) {
    console.log(`[MAILER INFO] SMTP settings not configured. Simulated email to <${to}>: "${subject}"`);
    return false;
  }

  try {
    const transporter = nodemailer.createTransport({
      host,
      port: process.env.SMTP_PORT || 587,
      secure: false,
      auth: { user, pass },
    });

    const info = await transporter.sendMail({
      from: `"Blood Bank Portal" <${user}>`,
      to,
      subject,
      text,
      html,
    });

    console.log(`[MAILER SUCCESS] Email delivered: ${info.messageId}`);
    return true;
  } catch (err) {
    // Non-blocking: never crash the server on email failure
    console.warn(`[MAILER WARNING] Failed to deliver email to ${to}: ${err.message}`);
    return false;
  }
}

// CLI test demo run: npm run demo:mail
if (require.main === module) {
  require('dotenv').config();
  console.log('Sending test email via demo:mail...');
  sendNotificationEmail({
    to: 'hospital_test@example.com',
    subject: 'Blood Request Test Notification',
    text: 'This is a test notification verifying the mailer service.',
    html: '<b>This is a test notification verifying the mailer service.</b>',
  }).then(() => console.log('Mail demo finished.'));
}

module.exports = { sendNotificationEmail };
