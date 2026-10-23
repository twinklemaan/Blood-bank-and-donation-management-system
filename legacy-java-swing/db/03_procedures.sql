-- =====================================================================
-- PROCEDURES  (MySQL 8.0.16+)   Database: blood_bank_db_er
-- Run after 02_functions.sql.
-- Every object is wrapped in its own DELIMITER // ... // DELIMITER ; block.
-- =====================================================================
USE blood_bank_db_er;

-- =====================================================================
-- 2. PROCEDURES
-- Every procedure that touches several rows runs in its own transaction
-- (START TRANSACTION implicitly commits anything the caller had open).
-- Lock order everywhere: request -> stock row -> donation rows.
-- =====================================================================

-- ---- registration --------------------------------------------------
DELIMITER //
CREATE PROCEDURE sp_register_bank (
  IN p_name VARCHAR(100), IN p_phone VARCHAR(15), IN p_city VARCHAR(50), IN p_state VARCHAR(50),
  IN p_pincode CHAR(6), IN p_licence_no VARCHAR(30), OUT p_bank_id INT)
BEGIN
  INSERT INTO blood_bank (name, licence_no, phone, city, state, pincode)
  VALUES (p_name, p_licence_no, p_phone, p_city, p_state, p_pincode);
  SET p_bank_id = LAST_INSERT_ID();
END //
DELIMITER ;

DELIMITER //
CREATE PROCEDURE sp_register_hospital (
  IN p_name VARCHAR(100), IN p_phone VARCHAR(15), IN p_city VARCHAR(50), IN p_state VARCHAR(50),
  IN p_pincode CHAR(6), IN p_reg_no VARCHAR(30), OUT p_hospital_id INT)
BEGIN
  INSERT INTO hospital (name, reg_no, phone, city, state, pincode)
  VALUES (p_name, p_reg_no, p_phone, p_city, p_state, p_pincode);
  SET p_hospital_id = LAST_INSERT_ID();
END //
DELIMITER ;

DELIMITER //
CREATE PROCEDURE sp_register_staff (
  IN p_bank_id INT, IN p_name VARCHAR(100), IN p_role VARCHAR(40),
  IN p_phone VARCHAR(15), OUT p_staff_id INT)
BEGIN
  INSERT INTO staff (bank_id, name, role, phone) VALUES (p_bank_id, p_name, p_role, p_phone);
  SET p_staff_id = LAST_INSERT_ID();
END //
DELIMITER ;

DELIMITER //
CREATE PROCEDURE sp_register_donor (
  IN p_name VARCHAR(100), IN p_gender CHAR(1), IN p_dob DATE, IN p_blood_group VARCHAR(3),
  IN p_city VARCHAR(50), IN p_state VARCHAR(50), IN p_pincode CHAR(6),
  IN p_phone VARCHAR(15), OUT p_donor_id INT)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF p_dob >= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Date of birth must be in the past';
  END IF;
  START TRANSACTION;
  INSERT INTO donor (name, gender, dob, blood_group, city, state, pincode)
  VALUES (p_name, p_gender, p_dob, p_blood_group, p_city, p_state, p_pincode);
  SET p_donor_id = LAST_INSERT_ID();
  INSERT INTO donor_phone (donor_id, phone) VALUES (p_donor_id, p_phone);   -- P1: at least one phone
  COMMIT;
END //
DELIMITER ;

-- ---- stock plumbing (called only from triggers and procedures) ------
-- Adding stock creates the (bank, group) row on first use (maintains is optional-many).
-- Removing stock requires the row; a result below 0 is rejected by CHECK (units_available >= 0).
DELIMITER //
CREATE PROCEDURE sp_adjust_stock (IN p_bank_id INT, IN p_group VARCHAR(3), IN p_delta INT)
BEGIN
  IF p_delta > 0 THEN
    INSERT INTO blood_stock (bank_id, blood_group, units_available)
    VALUES (p_bank_id, p_group, p_delta)
    ON DUPLICATE KEY UPDATE units_available = units_available + p_delta;
  ELSEIF p_delta < 0 THEN
    UPDATE blood_stock SET units_available = units_available + p_delta
     WHERE bank_id = p_bank_id AND blood_group = p_group;
    IF ROW_COUNT() = 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No stock row to deduct from';
    END IF;
  END IF;
END //
DELIMITER ;

-- ---- donations -----------------------------------------------------
-- All eligibility rules live in the triggers, so no insert path can bypass them.
-- p_date NULL = today.
DELIMITER //
CREATE PROCEDURE sp_record_donation (
  IN p_donor_id INT, IN p_bank_id INT, IN p_units INT, IN p_hb DECIMAL(4,1),
  IN p_result VARCHAR(7), IN p_date DATE, OUT p_donation_id INT)
BEGIN
  DECLARE v_group VARCHAR(3);
  DECLARE v_lock  INT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT blood_group INTO v_group FROM donor WHERE donor_id = p_donor_id;
  IF v_group IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unknown donor';
  END IF;

  START TRANSACTION;
  -- take the stock lock BEFORE inserting the donation (lock order: stock -> donation)
  INSERT IGNORE INTO blood_stock (bank_id, blood_group) VALUES (p_bank_id, v_group);
  SELECT COUNT(*) INTO v_lock FROM blood_stock
   WHERE bank_id = p_bank_id AND blood_group = v_group FOR UPDATE;

  INSERT INTO donation (donor_id, bank_id, donation_date, units, hemoglobin_level, screening_result)
  VALUES (p_donor_id, p_bank_id, IFNULL(p_date, CURDATE()), p_units, p_hb, p_result);
  SET p_donation_id = LAST_INSERT_ID();
  COMMIT;
END //
DELIMITER ;

-- Pending -> Pass | Fail. A Pass adds the units to stock exactly once (trg_donation_au).
DELIMITER //
CREATE PROCEDURE sp_update_screening (IN p_donation_id INT, IN p_result VARCHAR(7))
BEGIN
  DECLARE v_bank  INT;
  DECLARE v_group VARCHAR(3);
  DECLARE v_lock  INT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT d.bank_id, o.blood_group INTO v_bank, v_group
    FROM donation d JOIN donor o ON o.donor_id = d.donor_id
   WHERE d.donation_id = p_donation_id;
  IF v_bank IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unknown donation';
  END IF;

  START TRANSACTION;
  INSERT IGNORE INTO blood_stock (bank_id, blood_group) VALUES (v_bank, v_group);
  SELECT COUNT(*) INTO v_lock FROM blood_stock
   WHERE bank_id = v_bank AND blood_group = v_group FOR UPDATE;          -- stock first
  UPDATE donation SET screening_result = p_result WHERE donation_id = p_donation_id;
  COMMIT;
END //
DELIMITER ;

-- ---- expiry ---------------------------------------------------------
-- Core: zero the unused units of expired Pass donations for one bank/group.
-- Stock is corrected by trg_donation_au. No transaction control, so it can be
-- called from inside sp_fulfil_request.
DELIMITER //
CREATE PROCEDURE sp_expire_pair (IN p_bank_id INT, IN p_group VARCHAR(3))
BEGIN
  DECLARE v_lock INT;
  SELECT COUNT(*) INTO v_lock FROM blood_stock
   WHERE bank_id = p_bank_id AND blood_group = p_group FOR UPDATE;
  UPDATE donation d JOIN donor o ON o.donor_id = d.donor_id
     SET d.units_remaining = 0
   WHERE d.bank_id = p_bank_id
     AND o.blood_group = p_group
     AND d.screening_result = 'Pass'
     AND d.units_remaining > 0
     AND d.expiry_date < CURDATE();
END //
DELIMITER ;

-- Sweep every bank/group with expired units. Run daily by ev_expire_stock.
DELIMITER //
CREATE PROCEDURE sp_expire_all ()
BEGIN
  DECLARE v_done  TINYINT DEFAULT 0;
  DECLARE v_bank  INT;
  DECLARE v_group VARCHAR(3);
  DECLARE c CURSOR FOR
    SELECT DISTINCT d.bank_id, o.blood_group
      FROM donation d JOIN donor o ON o.donor_id = d.donor_id
     WHERE d.screening_result = 'Pass' AND d.units_remaining > 0 AND d.expiry_date < CURDATE()
     ORDER BY d.bank_id, o.blood_group;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  OPEN c;
  sweep: LOOP
    FETCH c INTO v_bank, v_group;
    IF v_done = 1 THEN LEAVE sweep; END IF;
    START TRANSACTION;
    CALL sp_expire_pair(v_bank, v_group);
    COMMIT;
  END LOOP;
  CLOSE c;
END //
DELIMITER ;

-- ---- requests --------------------------------------------------------
DELIMITER //
CREATE PROCEDURE sp_raise_request (
  IN p_hospital_id INT, IN p_bank_id INT, IN p_group VARCHAR(3),
  IN p_units INT, IN p_urgency VARCHAR(8), OUT p_request_id INT)
BEGIN
  INSERT INTO blood_request (hospital_id, bank_id, blood_group, units_requested, urgency)
  VALUES (p_hospital_id, p_bank_id, p_group, p_units, p_urgency);
  SET p_request_id = LAST_INSERT_ID();
END //
DELIMITER ;

-- Atomic check-and-deduct, verified by one staff member of the same bank.
--   p_hold_sec : DEMO ONLY. Holds the locks for n seconds so a second session can be
--                started while this one is still open (pass 0 in normal use).
--   p_outcome  : 'Fulfilled' or 'Rejected' (not enough stock, or no stock row).
-- Errors (request not Pending, wrong staff) roll back and leave the request Pending.
DELIMITER //
CREATE PROCEDURE sp_fulfil_request (
  IN p_request_id INT, IN p_staff_id INT, IN p_hold_sec INT, OUT p_outcome VARCHAR(10))
proc: BEGIN
  DECLARE v_found      TINYINT DEFAULT 1;
  DECLARE v_status     VARCHAR(9);
  DECLARE v_bank       INT;
  DECLARE v_group      VARCHAR(3);
  DECLARE v_units      INT;
  DECLARE v_staff_bank INT;
  DECLARE v_lock       INT;
  DECLARE v_avail      INT DEFAULT 0;
  DECLARE v_need       INT;
  DECLARE v_take       INT;
  DECLARE v_don        INT;
  DECLARE v_rem        INT;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_found = 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  START TRANSACTION;

  -- 1. serialise on the request row; only a Pending request may proceed
  SELECT status, bank_id, blood_group, units_requested
    INTO v_status, v_bank, v_group, v_units
    FROM blood_request WHERE request_id = p_request_id FOR UPDATE;
  IF v_found = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Request not found';
  END IF;
  IF v_status <> 'Pending' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Request is already processed';
  END IF;

  -- 2. the verifier must be staff of the bank that processes the request
  SET v_found = 1;
  SELECT bank_id INTO v_staff_bank FROM staff WHERE staff_id = p_staff_id;
  IF v_found = 0 OR v_staff_bank <> v_bank THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Verifying staff must work at the bank processing the request';
  END IF;

  -- 3. lock the stock row, drop expired units, then read the true figure
  SELECT COUNT(*) INTO v_lock FROM blood_stock
   WHERE bank_id = v_bank AND blood_group = v_group FOR UPDATE;
  IF p_hold_sec > 0 THEN DO SLEEP(p_hold_sec); END IF;               -- demo only
  IF v_lock = 1 THEN
    CALL sp_expire_pair(v_bank, v_group);
    SELECT units_available INTO v_avail FROM blood_stock
     WHERE bank_id = v_bank AND blood_group = v_group FOR UPDATE;
  END IF;

  -- 4. not enough stock: reject (no partial fulfilment, no re-routing)
  IF v_avail < v_units THEN
    UPDATE blood_request SET status = 'Rejected' WHERE request_id = p_request_id;
    COMMIT;
    SET p_outcome = 'Rejected';
    LEAVE proc;
  END IF;

  -- 5. take units from the earliest-expiring Pass donations, record who verified each issue
  SET v_need = v_units;
  WHILE v_need > 0 DO
    SET v_found = 1;
    SELECT d.donation_id, d.units_remaining INTO v_don, v_rem
      FROM donation d JOIN donor o ON o.donor_id = d.donor_id
     WHERE d.bank_id = v_bank AND o.blood_group = v_group
       AND d.screening_result = 'Pass' AND d.units_remaining > 0
       AND d.expiry_date >= CURDATE()
     ORDER BY d.expiry_date, d.donation_id
     LIMIT 1 FOR UPDATE OF d;
    IF v_found = 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock summary out of sync with donations';
    END IF;
    SET v_take = LEAST(v_need, v_rem);
    UPDATE donation SET units_remaining = units_remaining - v_take
     WHERE donation_id = v_don;                                      -- trg_donation_au lowers the stock
    INSERT INTO request_issue (request_id, donation_id, units_taken, staff_id, verified_on)
    VALUES (p_request_id, v_don, v_take, p_staff_id, CURDATE());
    SET v_need = v_need - v_take;
  END WHILE;

  UPDATE blood_request
     SET status = 'Fulfilled', fulfilled_date = NOW()
   WHERE request_id = p_request_id;
  COMMIT;
  SET p_outcome = 'Fulfilled';
END //
DELIMITER ;
