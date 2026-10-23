// validate.js - Client-side Regular Expressions & Input Sanitization (Unit 3: JS, DOM, RegExp)

/**
 * Strips HTML angle brackets (< and >) to prevent Cross-Site Scripting (XSS) injection.
 * SYLLABUS CONCEPT: Secure Sanitization
 */
function sanitize(input) {
  if (typeof input !== 'string') return '';
  return input.replace(/[<>]/g, '').trim();
}

/**
 * Regular Expression Patterns
 */
const REGEX_PATTERNS = {
  // Matches valid standard email format
  email: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  // Matches 10-digit phone number starting with 6, 7, 8, or 9
  phone: /^[6-9]\d{9}$/,
  // Matches exactly 6 numeric digits for Indian postal pincodes
  pincode: /^\d{6}$/,
  // Matches allowable standard ABO blood groups
  bloodGroup: /^(A|B|AB|O)[+-]$/,
};

/**
 * Validates an email address against regular expression
 */
function validateEmail(email) {
  return REGEX_PATTERNS.email.test(sanitize(email));
}

/**
 * Validates a 10-digit phone number
 */
function validatePhone(phone) {
  return REGEX_PATTERNS.phone.test(sanitize(phone));
}

/**
 * Validates a 6-digit postal pincode
 */
function validatePincode(pincode) {
  return REGEX_PATTERNS.pincode.test(sanitize(pincode));
}

/**
 * Validates standard blood group format
 */
function validateBloodGroup(group) {
  return REGEX_PATTERNS.bloodGroup.test(sanitize(group));
}

// Export for Node environments (testing) or attach to window in browser
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    sanitize,
    validateEmail,
    validatePhone,
    validatePincode,
    validateBloodGroup,
  };
}
