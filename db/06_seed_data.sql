USE blood_bank_db_er;

-- =====================================================================
-- 06_seed_data.sql - Seed data using stored procedures
-- =====================================================================

-- 1. Register 2 Banks
CALL sp_register_bank('City Central Blood Bank', '020-25670001', 'Pune', 'Maharashtra', '411001', 'LIC-PUN-001', @bank1_id);
CALL sp_register_bank('Metro Life Blood Bank', '022-26780002', 'Mumbai', 'Maharashtra', '400001', 'LIC-MUM-002', @bank2_id);

-- 2. Register 2 Hospitals
CALL sp_register_hospital('Ruby Care Hospital', '020-27890001', 'Pune', 'Maharashtra', '411002', 'REG-PUN-101', @hosp1_id);
CALL sp_register_hospital('Lilavati General Hospital', '022-28900002', 'Mumbai', 'Maharashtra', '400002', 'REG-MUM-102', @hosp2_id);

-- 3. Register 2 Staff per Bank
CALL sp_register_staff(@bank1_id, 'Ramesh Sharma', 'Lab Technician', '9822000001', @staff1_id);
CALL sp_register_staff(@bank1_id, 'Priya Patil', 'Medical Officer', '9822000002', @staff2_id);

CALL sp_register_staff(@bank2_id, 'Anand Verma', 'Lab Technician', '9823000001', @staff3_id);
CALL sp_register_staff(@bank2_id, 'Sneha Kulkarni', 'Nurse', '9823000002', @staff4_id);

-- 4. Register Donors covering all blood groups
CALL sp_register_donor('Amit Joshi', 'M', '1995-04-10', 'A+', 'Pune', 'Maharashtra', '411004', '9822100001', @d1);
CALL sp_register_donor('Bhavna Deshmukh', 'F', '1998-08-15', 'A-', 'Pune', 'Maharashtra', '411005', '9822100002', @d2);
CALL sp_register_donor('Chetan Shinde', 'M', '1990-12-05', 'B+', 'Mumbai', 'Maharashtra', '400004', '9823100003', @d3);
CALL sp_register_donor('Deepa Nair', 'F', '1993-02-20', 'B-', 'Mumbai', 'Maharashtra', '400005', '9823100004', @d4);
CALL sp_register_donor('Eshan Khan', 'M', '1988-06-12', 'AB+', 'Pune', 'Maharashtra', '411006', '9822100005', @d5);
CALL sp_register_donor('Farah Merchant', 'F', '1996-10-30', 'AB-', 'Mumbai', 'Maharashtra', '400006', '9823100006', @d6);
CALL sp_register_donor('Gaurav Kadam', 'M', '1992-03-25', 'O+', 'Pune', 'Maharashtra', '411007', '9822100007', @d7);
CALL sp_register_donor('Hema Rao', 'F', '1997-07-18', 'O-', 'Mumbai', 'Maharashtra', '400007', '9823100008', @d8);
CALL sp_register_donor('Ishaan Gupta', 'M', '1994-01-15', 'O+', 'Pune', 'Maharashtra', '411008', '9822100009', @d9);
CALL sp_register_donor('Jayesh Varma', 'M', '1991-05-22', 'A+', 'Mumbai', 'Maharashtra', '400008', '9823100010', @d10);

-- 5. Record Donations (dates between 1 and 30 days ago, units > 0, Hb >= 12.5)
-- Initially Pending, then screened
-- Donor 1 (A+) -> Bank 1, 10 days ago, 2 units -> Pass
CALL sp_record_donation(@d1, @bank1_id, 2, 14.0, 'Pending', DATE_SUB(CURDATE(), INTERVAL 10 DAY), @don1);
CALL sp_update_screening(@don1, 'Pass');

-- Donor 2 (A-) -> Bank 1, 15 days ago, 1 unit (Low stock!) -> Pass
CALL sp_record_donation(@d2, @bank1_id, 1, 13.5, 'Pending', DATE_SUB(CURDATE(), INTERVAL 15 DAY), @don2);
CALL sp_update_screening(@don2, 'Pass');

-- Donor 3 (B+) -> Bank 1, 5 days ago, 3 units -> Pass
CALL sp_record_donation(@d3, @bank1_id, 3, 14.5, 'Pending', DATE_SUB(CURDATE(), INTERVAL 5 DAY), @don3);
CALL sp_update_screening(@don3, 'Pass');

-- Donor 4 (B-) -> Bank 2, 20 days ago, 2 units (Low stock!) -> Pass
CALL sp_record_donation(@d4, @bank2_id, 2, 13.0, 'Pending', DATE_SUB(CURDATE(), INTERVAL 20 DAY), @don4);
CALL sp_update_screening(@don4, 'Pass');

-- Donor 5 (AB+) -> Bank 2, 8 days ago, 4 units -> Pass
CALL sp_record_donation(@d5, @bank2_id, 4, 15.0, 'Pending', DATE_SUB(CURDATE(), INTERVAL 8 DAY), @don5);
CALL sp_update_screening(@don5, 'Pass');

-- Donor 6 (AB-) -> Bank 2, 12 days ago, 1 unit (Low stock!) -> Pass
CALL sp_record_donation(@d6, @bank2_id, 1, 13.2, 'Pending', DATE_SUB(CURDATE(), INTERVAL 12 DAY), @don6);
CALL sp_update_screening(@don6, 'Pass');

-- Donor 7 (O+) -> Bank 1, 25 days ago, 3 units -> Pass
CALL sp_record_donation(@d7, @bank1_id, 3, 14.2, 'Pending', DATE_SUB(CURDATE(), INTERVAL 25 DAY), @don7);
CALL sp_update_screening(@don7, 'Pass');

-- Donor 8 (O-) -> Bank 2, 18 days ago, 2 units (Low stock!) -> Pass
CALL sp_record_donation(@d8, @bank2_id, 2, 13.8, 'Pending', DATE_SUB(CURDATE(), INTERVAL 18 DAY), @don8);
CALL sp_update_screening(@don8, 'Pass');

-- Donor 9 (O+) -> Bank 1, 3 days ago, 2 units -> Fail
CALL sp_record_donation(@d9, @bank1_id, 2, 13.0, 'Pending', DATE_SUB(CURDATE(), INTERVAL 3 DAY), @don9);
CALL sp_update_screening(@don9, 'Fail');

-- Donor 10 (A+) -> Bank 2, 2 days ago, 2 units -> Left Pending for staff screening demo
CALL sp_record_donation(@d10, @bank2_id, 2, 14.0, 'Pending', DATE_SUB(CURDATE(), INTERVAL 2 DAY), @don10);

-- 6. Insert App Users with SHA-256(salt + password)
-- admin / admin123
INSERT INTO app_user (username, salt, password_hash, role, staff_id, hospital_id)
VALUES ('admin', '0123456789abcdef', LOWER(SHA2(CONCAT('0123456789abcdef', 'admin123'), 256)), 'ADMIN', NULL, NULL);

-- staff1 / staff123 (linked to first staff of bank 1)
INSERT INTO app_user (username, salt, password_hash, role, staff_id, hospital_id)
VALUES ('staff1', '1122334455667788', LOWER(SHA2(CONCAT('1122334455667788', 'staff123'), 256)), 'STAFF', @staff1_id, NULL);

-- staff2 / staff123 (linked to first staff of bank 2)
INSERT INTO app_user (username, salt, password_hash, role, staff_id, hospital_id)
VALUES ('staff2', '2233445566778899', LOWER(SHA2(CONCAT('2233445566778899', 'staff123'), 256)), 'STAFF', @staff3_id, NULL);

-- hosp1 / hosp123 (linked to hospital 1)
INSERT INTO app_user (username, salt, password_hash, role, staff_id, hospital_id)
VALUES ('hosp1', '33445566778899aa', LOWER(SHA2(CONCAT('33445566778899aa', 'hosp123'), 256)), 'HOSPITAL', NULL, @hosp1_id);
