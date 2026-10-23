// rules.js - Business Rules Engine (Unit 4 & 6: Business logic validation)
// All business rules are defined as pure, testable functions with simple error handling.

const URGENCY_ORDER = {
  Critical: 1,
  Urgent: 2,
  Normal: 3,
};

/**
 * Rule 1: Donor age on donation date must be between 18 and 65 years.
 */
function checkDonorAge(dob, donationDate) {
  const birth = new Date(dob);
  const donation = new Date(donationDate);

  let age = donation.getFullYear() - birth.getFullYear();
  const monthDiff = donation.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && donation.getDate() < birth.getDate())) {
    age--;
  }

  if (age < 18 || age > 65) {
    throw new Error('Donor must be aged 18-65');
  }
  return true;
}

/**
 * Rule 2: Hemoglobin must be at least 12.5 g/dL.
 */
function checkHemoglobin(hb) {
  if (Number(hb) < 12.5) {
    throw new Error('Haemoglobin below 12.5 g/dL: donor deferred');
  }
  return true;
}

/**
 * Rule 3: Donation date cannot be in the future.
 */
function checkDonationDate(donationDate) {
  const donation = new Date(donationDate);
  const now = new Date();
  if (donation.getTime() > now.getTime()) {
    throw new Error('Donation date cannot be in the future');
  }
  return true;
}

/**
 * Rule 4: 90 days gap required since donor's last non-failed donation.
 */
function checkDonationGap(lastDonationDate, newDonationDate) {
  if (!lastDonationDate) return true;

  const last = new Date(lastDonationDate).getTime();
  const current = new Date(newDonationDate).getTime();
  const diffDays = Math.abs(current - last) / (1000 * 60 * 60 * 24);

  if (diffDays < 90) {
    throw new Error('Donor must wait 90 days between donations');
  }
  return true;
}

/**
 * Rule 5: Calculate unit shelf life. Expiry date = donation date + 35 days.
 */
function calculateExpiryDate(donationDate) {
  const expiry = new Date(donationDate);
  expiry.setDate(expiry.getDate() + 35);
  return expiry;
}

/**
 * Rule 6: Screening state machine.
 * Only Pending -> Pass or Pending -> Fail is permitted. Pass/Fail cannot be changed.
 */
function validateScreeningTransition(currentStatus, newStatus) {
  if (currentStatus !== 'Pending') {
    throw new Error('A Pass or Fail screening result is final');
  }
  if (newStatus !== 'Pass' && newStatus !== 'Fail') {
    throw new Error('Screening result can only be changed to Pass or Fail');
  }
  return true;
}

/**
 * Rule 7: Only 'Pass' donations are considered eligible stock.
 */
function isStockEligible(donation) {
  const today = new Date();
  return (
    donation.screeningResult === 'Pass' &&
    donation.unitsRemaining > 0 &&
    new Date(donation.expiryDate) >= today
  );
}

/**
 * Rule 8: Fulfil Request using First-Expiry-First-Out (FEFO) strategy.
 * - Available donations must be Pass units with remaining stock, sorted by earliest expiryDate.
 * - If total units < requested units, the request is marked Rejected.
 * - Otherwise, deduct units in order, record issues array, and mark as Fulfilled.
 */
function fulfilRequestFEFO(request, availableDonations) {
  if (request.status !== 'Pending') {
    throw new Error('Only Pending requests can be fulfilled or rejected');
  }

  // Calculate sum of available units
  const totalAvailable = availableDonations.reduce((sum, d) => sum + d.unitsRemaining, 0);

  if (totalAvailable < request.unitsRequested) {
    return {
      canFulfil: false,
      status: 'Rejected',
      issues: [],
      donationUpdates: [],
    };
  }

  let unitsNeeded = request.unitsRequested;
  const issues = [];
  const donationUpdates = [];

  for (const donation of availableDonations) {
    if (unitsNeeded <= 0) break;
    if (donation.unitsRemaining <= 0) continue;

    const taken = Math.min(unitsNeeded, donation.unitsRemaining);
    const newRemaining = donation.unitsRemaining - taken;
    unitsNeeded -= taken;

    issues.push({
      donationId: donation._id,
      unitsTaken: taken,
    });

    donationUpdates.push({
      _id: donation._id,
      unitsRemaining: newRemaining,
    });
  }

  return {
    canFulfil: true,
    status: 'Fulfilled',
    issues,
    donationUpdates,
  };
}

/**
 * Rule 9: Sort hospital requests for staff: Critical -> Urgent -> Normal -> oldest first.
 */
function sortHospitalRequests(requests) {
  return requests.sort((a, b) => {
    const priorityA = URGENCY_ORDER[a.urgency] || 99;
    const priorityB = URGENCY_ORDER[b.urgency] || 99;

    if (priorityA !== priorityB) {
      return priorityA - priorityB;
    }
    // Secondary sort: oldest request date first
    return new Date(a.requestDate) - new Date(b.requestDate);
  });
}

module.exports = {
  checkDonorAge,
  checkHemoglobin,
  checkDonationDate,
  checkDonationGap,
  calculateExpiryDate,
  validateScreeningTransition,
  isStockEligible,
  fulfilRequestFEFO,
  sortHospitalRequests,
};
