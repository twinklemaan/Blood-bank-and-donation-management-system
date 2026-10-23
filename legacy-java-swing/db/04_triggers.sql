-- =====================================================================
-- TRIGGERS and the daily expiry event  (MySQL 8.0.16+)   Database: blood_bank_db_er
-- Run after 03_procedures.sql (the triggers call sp_adjust_stock).
-- Every object is wrapped in its own DELIMITER // ... // DELIMITER ; block.
-- =====================================================================
USE blood_bank_db_er;

-- =====================================================================
-- 3. TRIGGERS
-- =====================================================================

-- DONOR: blood group is immutable once the donor has donations
DELIMITER //
CREATE TRIGGER trg_donor_bu BEFORE UPDATE ON donor FOR EACH ROW
BEGIN
  DECLARE v_cnt INT;
  IF NEW.blood_group <> OLD.blood_group THEN
    SELECT COUNT(*) INTO v_cnt FROM donation WHERE donor_id = OLD.donor_id;
    IF v_cnt > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Blood group cannot change once a donor has donations';
    END IF;
  END IF;
END //
DELIMITER ;

-- DONATION: eligibility (age 18-65, Hb >= 12.5, 90-day gap) and Units_Remaining
DELIMITER //
CREATE TRIGGER trg_donation_bi BEFORE INSERT ON donation FOR EACH ROW
BEGIN
  DECLARE v_dob DATE;
  DECLARE v_cnt INT;

  SET NEW.units_remaining = NEW.units;

  IF NEW.donation_date > CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Donation date cannot be in the future';
  END IF;

  IF NEW.hemoglobin_level < 12.5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Haemoglobin below 12.5 g/dL: donor deferred';
  END IF;

  -- locking read: two donations for the same donor are checked one after the other
  SELECT dob INTO v_dob FROM donor WHERE donor_id = NEW.donor_id FOR UPDATE;
  IF v_dob IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unknown donor';
  END IF;
  IF TIMESTAMPDIFF(YEAR, v_dob, NEW.donation_date) NOT BETWEEN 18 AND 65 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Donor must be aged 18-65';
  END IF;

  -- 90-day gap; failed-screening donations do not count (either way round)
  IF NEW.screening_result <> 'Fail' THEN
    SELECT COUNT(*) INTO v_cnt FROM donation
     WHERE donor_id = NEW.donor_id AND screening_result <> 'Fail'
       AND ABS(DATEDIFF(NEW.donation_date, donation_date)) < 90
       FOR SHARE;
    IF v_cnt > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Donor must wait 90 days between donations';
    END IF;
  END IF;
END //
DELIMITER ;

-- DONATION: a donation inserted directly as Pass adds stock
DELIMITER //
CREATE TRIGGER trg_donation_ai AFTER INSERT ON donation FOR EACH ROW
BEGIN
  DECLARE v_group VARCHAR(3);
  IF NEW.screening_result = 'Pass' THEN
    SELECT blood_group INTO v_group FROM donor WHERE donor_id = NEW.donor_id;
    CALL sp_adjust_stock(NEW.bank_id, v_group, NEW.units_remaining);
  END IF;
END //
DELIMITER ;

-- DONATION: immutability and the screening state machine
DELIMITER //
CREATE TRIGGER trg_donation_bu BEFORE UPDATE ON donation FOR EACH ROW
BEGIN
  IF NEW.donor_id <> OLD.donor_id OR NEW.bank_id <> OLD.bank_id
     OR NEW.units <> OLD.units OR NEW.donation_date <> OLD.donation_date
     OR NEW.hemoglobin_level <> OLD.hemoglobin_level THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Donor, bank, units, date and haemoglobin are fixed once recorded';
  END IF;

  -- Pending -> Pass | Fail only; Pass and Fail are final
  IF OLD.screening_result <> NEW.screening_result AND OLD.screening_result <> 'Pending' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A Pass or Fail screening result is final';
  END IF;

  IF NEW.units_remaining > OLD.units_remaining THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Units can only be consumed, never added back';
  END IF;

  IF NEW.units_remaining < OLD.units_remaining AND OLD.screening_result <> 'Pass' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only a Pass donation can be issued or expired';
  END IF;
END //
DELIMITER ;

-- DONATION: the single place where stock changes after insert
DELIMITER //
CREATE TRIGGER trg_donation_au AFTER UPDATE ON donation FOR EACH ROW
BEGIN
  DECLARE v_group VARCHAR(3);
  DECLARE v_delta INT DEFAULT 0;
  IF OLD.screening_result = 'Pending' AND NEW.screening_result = 'Pass' THEN
    SET v_delta = NEW.units_remaining;                          -- added once, on the transition
  ELSEIF OLD.screening_result = 'Pass' AND NEW.screening_result = 'Pass' THEN
    SET v_delta = NEW.units_remaining - OLD.units_remaining;    -- issue or expiry (<= 0)
  END IF;
  IF v_delta <> 0 THEN
    SELECT blood_group INTO v_group FROM donor WHERE donor_id = NEW.donor_id;
    CALL sp_adjust_stock(NEW.bank_id, v_group, v_delta);
  END IF;
END //
DELIMITER ;

-- DONATION: a Pass donation backs stock and may back issue records, so it is permanent
DELIMITER //
CREATE TRIGGER trg_donation_bd BEFORE DELETE ON donation FOR EACH ROW
BEGIN
  IF OLD.screening_result = 'Pass' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A Pass donation cannot be deleted';
  END IF;
END //
DELIMITER ;

-- BLOOD_REQUEST: always starts Pending
DELIMITER //
CREATE TRIGGER trg_request_bi BEFORE INSERT ON blood_request FOR EACH ROW
BEGIN
  IF NEW.status <> 'Pending' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A new request must be Pending';
  END IF;
END //
DELIMITER ;

-- BLOOD_REQUEST: Pending is the only editable state; Fulfilled must match its issue records
DELIMITER //
CREATE TRIGGER trg_request_bu BEFORE UPDATE ON blood_request FOR EACH ROW
BEGIN
  DECLARE v_issued INT;
  IF OLD.status <> 'Pending' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Request is already processed and cannot change';
  END IF;
  IF NEW.hospital_id <> OLD.hospital_id OR NEW.bank_id <> OLD.bank_id
     OR NEW.blood_group <> OLD.blood_group OR NEW.units_requested <> OLD.units_requested THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hospital, bank, blood group and units cannot change once raised';
  END IF;
  IF NEW.status = 'Fulfilled' THEN
    SELECT IFNULL(SUM(units_taken), 0) INTO v_issued FROM request_issue WHERE request_id = OLD.request_id;
    IF v_issued <> OLD.units_requested THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Fulfilled requests must have exactly the requested units issued';
    END IF;
  END IF;
END //
DELIMITER ;

-- REQUEST_ISSUE (issued_from + verifies): cross-table rules a CHECK cannot express
DELIMITER //
CREATE TRIGGER trg_issue_bi BEFORE INSERT ON request_issue FOR EACH ROW
BEGIN
  DECLARE v_ok     INT;
  DECLARE v_rdate  DATE;
  DECLARE v_total  INT;
  DECLARE v_asked  INT;

  -- donation: Pass, same bank and blood group as the request; verifier: staff of that same bank
  SELECT COUNT(*) INTO v_ok
    FROM blood_request r
    JOIN donation d ON d.donation_id = NEW.donation_id
    JOIN donor    o ON o.donor_id    = d.donor_id
    JOIN staff    s ON s.staff_id    = NEW.staff_id
   WHERE r.request_id       = NEW.request_id
     AND d.bank_id          = r.bank_id
     AND o.blood_group      = r.blood_group
     AND d.screening_result = 'Pass'
     AND s.bank_id          = r.bank_id;
  IF v_ok = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
      'Donation must be a Pass from the request''s bank and blood group, verified by staff of that bank';
  END IF;

  SELECT DATE(request_date), units_requested INTO v_rdate, v_asked
    FROM blood_request WHERE request_id = NEW.request_id;
  IF NEW.verified_on < v_rdate OR NEW.verified_on > CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verified_On must be between the request date and today';
  END IF;

  SELECT IFNULL(SUM(units_taken), 0) INTO v_total FROM request_issue WHERE request_id = NEW.request_id;
  IF v_total + NEW.units_taken > v_asked THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'More units issued than the request asked for';
  END IF;
END //
DELIMITER ;

-- REQUEST_ISSUE: issue records are an audit trail
DELIMITER //
CREATE TRIGGER trg_issue_bu BEFORE UPDATE ON request_issue FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Issue records cannot be changed';
END //
DELIMITER ;

DELIMITER //
CREATE TRIGGER trg_issue_bd BEFORE DELETE ON request_issue FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Issue records cannot be deleted';
END //
DELIMITER ;

-- =====================================================================
-- 4. DAILY EXPIRY SWEEP (needs the event scheduler: SET GLOBAL event_scheduler = ON;)
-- Note: until the sweep runs, v_bank_availability can still count units that
-- expired today; sp_fulfil_request always sweeps first, so it never issues them.
-- =====================================================================
CREATE EVENT ev_expire_stock
  ON SCHEDULE EVERY 1 DAY STARTS (TIMESTAMP(CURRENT_DATE) + INTERVAL 1 DAY + INTERVAL 5 MINUTE)
  DO CALL sp_expire_all();
