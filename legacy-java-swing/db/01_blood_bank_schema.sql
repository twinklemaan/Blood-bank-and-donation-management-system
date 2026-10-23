-- =====================================================================
-- BLOOD BANK & DONATION MANAGEMENT SYSTEM  -  RELATIONAL SCHEMA (ER)
-- MySQL 8.0.16+ (CHECK constraints are enforced from 8.0.16)
-- Order: tables, indexes, views. Triggers and procedures are a separate script.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS blood_bank_db_er;
USE blood_bank_db_er;

-- ---------- BLOOD_BANK and HOSPITAL (separate entities in the ER) ----------
CREATE TABLE blood_bank (
  bank_id    INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  licence_no VARCHAR(30)  NOT NULL UNIQUE,
  phone      VARCHAR(15)  NOT NULL,
  city       VARCHAR(50)  NOT NULL,
  state      VARCHAR(50)  NOT NULL,
  pincode    CHAR(6)      NOT NULL,
  CONSTRAINT ck_bank_pin CHECK (pincode REGEXP '^[0-9]{6}$')
);

CREATE TABLE hospital (
  hospital_id INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  reg_no      VARCHAR(30)  NOT NULL UNIQUE,
  phone       VARCHAR(15)  NOT NULL,
  city        VARCHAR(50)  NOT NULL,
  state       VARCHAR(50)  NOT NULL,
  pincode     CHAR(6)      NOT NULL,
  CONSTRAINT ck_hosp_pin CHECK (pincode REGEXP '^[0-9]{6}$')
);

-- ---------- STAFF (works_at: BLOOD_BANK mandatory one, STAFF optional many) ----------
CREATE TABLE staff (
  staff_id INT AUTO_INCREMENT PRIMARY KEY,
  bank_id  INT          NOT NULL,
  name     VARCHAR(100) NOT NULL,
  role     VARCHAR(40)  NOT NULL,
  phone    VARCHAR(15)  NOT NULL,
  CONSTRAINT fk_staff_bank FOREIGN KEY (bank_id) REFERENCES blood_bank (bank_id)
);


-- ---------- DONOR (composite Address flattened; multivalued Phone in its own table) ----------
CREATE TABLE donor (
  donor_id    INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  gender      CHAR(1)      NOT NULL,
  dob         DATE         NOT NULL,
  blood_group VARCHAR(3)   NOT NULL,
  city        VARCHAR(50)  NOT NULL,
  state       VARCHAR(50)  NOT NULL,
  pincode     CHAR(6)      NOT NULL,
  CONSTRAINT ck_donor_gender CHECK (gender IN ('M','F','O')),
  CONSTRAINT ck_donor_bg     CHECK (blood_group IN ('A+','A-','B+','B-','AB+','AB-','O+','O-')),
  CONSTRAINT ck_donor_pin    CHECK (pincode REGEXP '^[0-9]{6}$')
);

CREATE TABLE donor_phone (
  donor_id INT         NOT NULL,
  phone    VARCHAR(15) NOT NULL,
  CONSTRAINT pk_donor_phone PRIMARY KEY (donor_id, phone),
  CONSTRAINT fk_phone_donor FOREIGN KEY (donor_id) REFERENCES donor (donor_id) ON DELETE CASCADE
);

-- ---------- DONATION (makes + collects; Expiry_Date is a derived attribute) ----------
CREATE TABLE donation (
  donation_id      INT AUTO_INCREMENT PRIMARY KEY,
  donor_id         INT          NOT NULL,
  bank_id          INT          NOT NULL,
  donation_date    DATE         NOT NULL DEFAULT (CURRENT_DATE),
  units            INT          NOT NULL,
  units_remaining  INT          NOT NULL DEFAULT 0,      -- set equal to units by the insert trigger
  hemoglobin_level DECIMAL(4,1) NOT NULL,
  screening_result VARCHAR(7)   NOT NULL DEFAULT 'Pending',
  expiry_date      DATE GENERATED ALWAYS AS (DATE_ADD(donation_date, INTERVAL 35 DAY)) VIRTUAL,
  CONSTRAINT ck_don_units  CHECK (units > 0),
  CONSTRAINT ck_don_rem    CHECK (units_remaining BETWEEN 0 AND units),
  CONSTRAINT ck_don_hb     CHECK (hemoglobin_level BETWEEN 5 AND 25),
  CONSTRAINT ck_don_result CHECK (screening_result IN ('Pass','Fail','Pending')),
  CONSTRAINT fk_don_donor  FOREIGN KEY (donor_id) REFERENCES donor (donor_id),
  CONSTRAINT fk_don_bank   FOREIGN KEY (bank_id)  REFERENCES blood_bank (bank_id)
);

-- ---------- BLOOD_STOCK (weak entity of BLOOD_BANK: key = bank_id + blood_group) ----------
CREATE TABLE blood_stock (
  bank_id         INT        NOT NULL,
  blood_group     VARCHAR(3) NOT NULL,
  units_available INT        NOT NULL DEFAULT 0,
  last_updated    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_blood_stock PRIMARY KEY (bank_id, blood_group),
  CONSTRAINT ck_stock_bg    CHECK (blood_group IN ('A+','A-','B+','B-','AB+','AB-','O+','O-')),
  CONSTRAINT ck_stock_units CHECK (units_available >= 0),
  CONSTRAINT fk_stock_bank  FOREIGN KEY (bank_id) REFERENCES blood_bank (bank_id) ON DELETE CASCADE
);

-- ---------- BLOOD_REQUEST (raises + processes) ----------
CREATE TABLE blood_request (
  request_id      INT AUTO_INCREMENT PRIMARY KEY,
  hospital_id     INT         NOT NULL,
  bank_id         INT         NOT NULL,
  blood_group     VARCHAR(3)  NOT NULL,
  units_requested INT         NOT NULL,
  urgency         VARCHAR(8)  NOT NULL DEFAULT 'Normal',
  status          VARCHAR(9)  NOT NULL DEFAULT 'Pending',
  request_date    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fulfilled_date  DATETIME    NULL,
  CONSTRAINT ck_req_bg      CHECK (blood_group IN ('A+','A-','B+','B-','AB+','AB-','O+','O-')),
  CONSTRAINT ck_req_units   CHECK (units_requested > 0),
  CONSTRAINT ck_req_urgency CHECK (urgency IN ('Normal','Urgent','Critical')),
  CONSTRAINT ck_req_status  CHECK (status IN ('Pending','Fulfilled','Rejected')),
  CONSTRAINT ck_req_fdate   CHECK ((status = 'Fulfilled' AND fulfilled_date IS NOT NULL)
                                OR (status <> 'Fulfilled' AND fulfilled_date IS NULL)),
  CONSTRAINT fk_req_hosp    FOREIGN KEY (hospital_id) REFERENCES hospital (hospital_id),
  CONSTRAINT fk_req_bank    FOREIGN KEY (bank_id)     REFERENCES blood_bank (bank_id)
);

-- ---------- REQUEST_ISSUE = issued_from (M:N) + verifies (aggregation) ----------
-- Key of the aggregation = (request_id, donation_id).
-- verifies is 1:N (STAFF mandatory one, aggregation optional many), so it is folded
-- into this table as staff_id + verified_on, both NOT NULL.
CREATE TABLE request_issue (
  request_id  INT  NOT NULL,
  donation_id INT  NOT NULL,
  units_taken INT  NOT NULL,
  staff_id    INT  NOT NULL,
  verified_on DATE NOT NULL,
  CONSTRAINT pk_request_issue PRIMARY KEY (request_id, donation_id),
  CONSTRAINT ck_issue_units   CHECK (units_taken > 0),
  CONSTRAINT fk_issue_req     FOREIGN KEY (request_id)  REFERENCES blood_request (request_id),
  CONSTRAINT fk_issue_don     FOREIGN KEY (donation_id) REFERENCES donation (donation_id),
  CONSTRAINT fk_issue_staff   FOREIGN KEY (staff_id)    REFERENCES staff (staff_id)
);

-- ---------- INDEXES (P10 search paths and the oldest-first issue query) ----------
CREATE INDEX ix_donor_bg_city  ON donor (blood_group, city);
CREATE INDEX ix_bank_city ON blood_bank (city);
CREATE INDEX ix_stock_group    ON blood_stock (blood_group, units_available);
CREATE INDEX ix_donation_fefo  ON donation (bank_id, screening_result, expiry_date);
CREATE INDEX ix_request_bank   ON blood_request (bank_id, status);

-- ---------- VIEWS (derived attributes are computed, never stored) ----------
CREATE VIEW v_donor_status AS
SELECT d.donor_id, d.name, d.blood_group, d.city,
       TIMESTAMPDIFF(YEAR, d.dob, CURDATE()) AS age,
       (SELECT DATE_ADD(MAX(x.donation_date), INTERVAL 90 DAY)
          FROM donation x
         WHERE x.donor_id = d.donor_id AND x.screening_result <> 'Fail') AS next_eligible_date
  FROM donor d;

CREATE VIEW v_donation_detail AS
SELECT n.donation_id, n.donor_id, d.blood_group, n.bank_id, n.donation_date,
       n.expiry_date, n.units, n.units_remaining, n.hemoglobin_level, n.screening_result
  FROM donation n JOIN donor d ON d.donor_id = n.donor_id;

CREATE VIEW v_bank_availability AS
SELECT b.bank_id, b.name AS bank_name, b.city, s.blood_group, s.units_available
  FROM blood_stock s
  JOIN blood_bank b ON b.bank_id = s.bank_id
  ;
