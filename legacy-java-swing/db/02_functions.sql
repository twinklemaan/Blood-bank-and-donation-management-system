-- =====================================================================
-- FUNCTIONS  (MySQL 8.0.16+)   Database: blood_bank_db_er
-- Run after 01_blood_bank_schema.sql.
-- Every object is wrapped in its own DELIMITER // ... // DELIMITER ; block.
-- =====================================================================
USE blood_bank_db_er;

-- =====================================================================
-- 1. FUNCTIONS
-- =====================================================================
DELIMITER //
CREATE FUNCTION fn_age_on (p_dob DATE, p_on DATE) RETURNS INT
  DETERMINISTIC NO SQL
BEGIN
  RETURN TIMESTAMPDIFF(YEAR, p_dob, p_on);
END //
DELIMITER ;

DELIMITER //
CREATE FUNCTION fn_donor_age (p_donor_id INT) RETURNS INT
  READS SQL DATA
BEGIN
  DECLARE v_dob DATE;
  SELECT dob INTO v_dob FROM donor WHERE donor_id = p_donor_id;
  RETURN fn_age_on(v_dob, CURDATE());                 -- NULL for an unknown donor
END //
DELIMITER ;

-- Derived attribute Next_Eligible_Date: latest non-failed donation + 90 days.
-- NULL = never donated = eligible immediately.
DELIMITER //
CREATE FUNCTION fn_next_eligible_date (p_donor_id INT) RETURNS DATE
  READS SQL DATA
BEGIN
  DECLARE v_last DATE;
  SELECT MAX(donation_date) INTO v_last
    FROM donation
   WHERE donor_id = p_donor_id AND screening_result <> 'Fail';
  RETURN IF(v_last IS NULL, NULL, DATE_ADD(v_last, INTERVAL 90 DAY));
END //
DELIMITER ;

-- 1 = eligible on p_on (age 18-65 and 90-day gap satisfied), else 0.
DELIMITER //
CREATE FUNCTION fn_is_eligible (p_donor_id INT, p_on DATE) RETURNS TINYINT
  READS SQL DATA
BEGIN
  DECLARE v_dob  DATE;
  DECLARE v_next DATE;
  SELECT dob INTO v_dob FROM donor WHERE donor_id = p_donor_id;
  IF v_dob IS NULL THEN RETURN 0; END IF;
  IF fn_age_on(v_dob, p_on) NOT BETWEEN 18 AND 65 THEN RETURN 0; END IF;
  SET v_next = fn_next_eligible_date(p_donor_id);
  IF v_next IS NOT NULL AND v_next > p_on THEN RETURN 0; END IF;
  RETURN 1;
END //
DELIMITER ;

-- What BLOOD_STOCK.units_available SHOULD be (used to verify the invariant).
DELIMITER //
CREATE FUNCTION fn_stock_computed (p_bank_id INT, p_group VARCHAR(3)) RETURNS INT
  READS SQL DATA
BEGIN
  DECLARE v_sum INT;
  SELECT IFNULL(SUM(d.units_remaining), 0) INTO v_sum
    FROM donation d JOIN donor o ON o.donor_id = d.donor_id
   WHERE d.bank_id = p_bank_id AND o.blood_group = p_group
     AND d.screening_result = 'Pass';
  RETURN v_sum;
END //
DELIMITER ;
