# Blood Bank & Donation Management System (Java Swing + JDBC)

A comprehensive desktop blood banking and donation management application featuring a role-based Java Swing graphical user interface and a transactional MySQL 8.0+ backend (`blood_bank_db_er`).

---

## 1. System Overview

The system manages the entire lifecycle of donated blood:
$$\text{Donor Registration} \longrightarrow \text{Donation Intake (Pending)} \longrightarrow \text{Screening (Pass/Fail)} \longrightarrow \text{Inventory Stock} \longrightarrow \text{Hospital Requisition} \longrightarrow \text{FEFO Fulfilment} \longrightarrow \text{Daily Expiry Sweep}$$

- **Backend-Enforced Business Rules:** All transactions, concurrency locks, stock invariants, and medical eligibility constraints are authoritatively enforced by database stored procedures, functions, and triggers. The Java application serves as a clean, responsive front end calling these procedures via JDBC.
- **Role-Based Access Control:** Distinct workflows and interfaces for **System Administrators**, **Blood Bank Staff**, and **Hospital Representatives**.
- **10-Mark College Mini-Project Rubric Compliance:** Strictly implements Object-Oriented Inheritance, Polymorphism, Collections Framework, Custom Exception Handling, PreparedStatement/CallableStatement JDBC CRUD, Swing GUI, and Multithreading.

---

## 2. Prerequisites

1. **Java Development Kit (JDK):** JDK 17 or higher (tested on OpenJDK 17 and OpenJDK 25).
2. **MySQL Database Server:** MySQL Community Server 8.0.16+ (requires native SQL `CHECK` constraint enforcement).
3. **MySQL Connector/J:** Included in `lib/mysql-connector-j-26.7.0.jar`.

---

## 3. Database Setup Instructions

Follow these exact steps in MySQL Workbench or via the `mysql` CLI:

### Step 1: Execute Database SQL Scripts in Order
Execute the SQL scripts located in the `db/` directory in sequential order:

```bash
# 1. Base relational schema (tables, views, indexes, check constraints)
mysql -u root -p < db/01_blood_bank_schema.sql

# 2. Deterministic & data-reading stored functions
mysql -u root -p < db/02_functions.sql

# 3. Transactional stored procedures (registration, donation, fulfilment)
mysql -u root -p < db/03_procedures.sql

# 4. Triggers (eligibility, screening immutability, FEFO audit) and event
mysql -u root -p < db/04_triggers.sql

# 5. Application authentication and user credentials table
mysql -u root -p < db/05_app_user.sql

# 6. Seed data using stored procedures
mysql -u root -p < db/06_seed_data.sql
```

> **Note on Delimiters:** Files `02` through `04` use `DELIMITER //`. When using MySQL Workbench, open each script file and click the lightning bolt ("Execute").

### Step 2: Enable MySQL Event Scheduler
The daily automated blood unit expiry sweep requires the MySQL event scheduler:
```sql
SET GLOBAL event_scheduler = ON;
```
*(To make this setting permanent across server restarts, add `event_scheduler=ON` under `[mysqld]` in your `my.ini` or `my.cnf` file).*

### Step 3: Configure Database Credentials
Copy the example configuration file:
- On Windows: `copy config\db.properties.example config\db.properties`
- On Linux/macOS: `cp config/db.properties.example config/db.properties`

Edit `config/db.properties` with your MySQL server credentials:
```properties
db.url=jdbc:mysql://localhost:3306/blood_bank_db_er?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
db.user=root
db.password=YOUR_PASSWORD_HERE
stock.low.threshold=5
monitor.interval.seconds=10
demo.hold.seconds=0
```

---

## 4. How to Compile and Run

### On Windows
```cmd
# Compile all source files into out/
build.bat

# Compile and launch the application
build.bat run
```

### On Linux / macOS
```bash
chmod +x build.sh

# Compile
./build.sh

# Compile and run
./build.sh run
```

### Manual Command Line (Any OS)
```bash
# Compile:
javac -encoding UTF-8 -cp "lib/*" -d out src/com/bloodbank/*.java src/com/bloodbank/*/*.java

# Run:
java -cp "out;lib/*" com.bloodbank.Main        # Windows
java -cp "out:lib/*" com.bloodbank.Main        # Linux/macOS
```

---

## 5. Demo Logins

The database seed script (`db/06_seed_data.sql`) automatically provisions demo accounts with salted SHA-256 password hashes:

| Role | Username | Password | Organization / Linked Entity | Accessible Tabs |
|---|---|---|---|---|
| **Administrator** | `admin` | `admin123` | System-wide Root Access | Banks, Hospitals, Staff, Stock, All Donations, All Requests |
| **Bank Staff (Bank 1)** | `staff1` | `staff123` | City Central Blood Bank (Pune) | Donors, Donations, Screening, Requests to Fulfil, Stock |
| **Bank Staff (Bank 2)** | `staff2` | `staff123` | Metro Life Blood Bank (Mumbai) | Donors, Donations, Screening, Requests to Fulfil, Stock |
| **Hospital** | `hosp1` | `hosp123` | Ruby Care Hospital (Pune) | Raise Request, My Requests, Availability Search |

---

## 6. Project Directory Tour

```
BloodBankApp/
├── BRIEF.md                    <- Project specification and rubrics
├── README.md                   <- Setup, run instructions, and demo credentials
├── .gitignore                  <- Excludes compiled classes and private credentials
├── build.bat / build.sh        <- Automated compilation and launch scripts
├── config/
│   ├── db.properties.example   <- Committed template configuration
│   └── db.properties           <- Ignored local database credentials
├── db/
│   ├── 01_blood_bank_schema.sql <- Tables, views, and indexes
│   ├── 02_functions.sql         <- Deterministic & eligibility stored functions
│   ├── 03_procedures.sql        <- Transactional stored procedures
│   ├── 04_triggers.sql          <- State immutability & audit triggers
│   ├── 05_app_user.sql          <- Application credentials table with check constraints
│   └── 06_seed_data.sql         <- Stored procedure based seed dataset
├── lib/
│   └── mysql-connector-j-26.7.0.jar <- MySQL JDBC driver
├── docs/
│   ├── DESIGN.md               <- System architecture, roles, and Mermaid class diagrams
│   ├── RUBRIC_MAPPING.md       <- 10-mark college rubric traceability matrix
│   ├── VIVA_NOTES.md           <- Comprehensive viva questions and technical explanations
│   └── screenshots/            <- UI demonstration screenshots
└── src/com/bloodbank/
    ├── Main.java               <- Application startup entry point
    ├── exception/              <- Custom exception hierarchy (App, Validation, Database, Auth)
    ├── model/                  <- OOP models (User, Person, Bank, Hospital, Donation, Request)
    ├── dao/                    <- JDBC DAOs (BaseDao, Auth, Bank, Hospital, Staff, Donor, etc.)
    ├── ui/                     <- Swing presentation layer (LoginFrame, MainFrame, tab panels)
    ├── thread/                 <- Background stock polling thread (StockMonitor, StockListener)
    └── util/                   <- Utilities (DBConnection, PasswordUtil, ErrorMapper, Validator)
```
