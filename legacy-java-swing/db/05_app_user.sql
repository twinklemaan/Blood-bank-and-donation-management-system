USE blood_bank_db_er;

CREATE TABLE IF NOT EXISTS app_user (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50) NOT NULL UNIQUE,
  salt          CHAR(16)    NOT NULL,
  password_hash CHAR(64)    NOT NULL,          -- lowercase hex SHA-256(salt + password)
  role          VARCHAR(10) NOT NULL,
  staff_id      INT NULL,
  hospital_id   INT NULL,
  CONSTRAINT ck_user_role CHECK (role IN ('ADMIN','STAFF','HOSPITAL')),
  CONSTRAINT ck_user_link CHECK (
       (role='ADMIN'    AND staff_id IS NULL     AND hospital_id IS NULL)
    OR (role='STAFF'    AND staff_id IS NOT NULL AND hospital_id IS NULL)
    OR (role='HOSPITAL' AND hospital_id IS NOT NULL AND staff_id IS NULL)),
  CONSTRAINT fk_user_staff FOREIGN KEY (staff_id)    REFERENCES staff (staff_id),
  CONSTRAINT fk_user_hosp  FOREIGN KEY (hospital_id) REFERENCES hospital (hospital_id)
);
