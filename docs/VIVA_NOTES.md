# Viva Voce & Technical Defense Notes

This document prepares the student for questions on database design, concurrency, Java object-oriented principles, and application architecture.

---

### Q1: Why are business rules and constraints implemented in Stored Procedures and Triggers rather than in Java code?
**Answer:**
Enforcing rules in the database ensures **absolute data integrity** regardless of what client connects (Java desktop app, web interface, mobile app, or direct CLI admin scripts). If business logic is in Java, a bug, bypassed validation, or direct SQL execution could corrupt inventory numbers or violate medical rules. Furthermore, stored procedures execute directly on the database engine, eliminating unnecessary network round-trips when checking eligibility, acquiring locks, and updating multiple tables atomically.

---

### Q2: What specific rules does each database trigger enforce?
**Answer:**
1. **`trg_donor_bu` (BEFORE UPDATE on `donor`):** Makes `blood_group` immutable once a donor has at least one recorded donation.
2. **`trg_donation_bi` (BEFORE INSERT on `donation`):** Validates donor age ($18 \le \text{age} \le 65$), minimum haemoglobin ($\ge 12.5\text{ g/dL}$), prevents future donation dates, and enforces the mandatory 90-day cooldown between non-failed donations.
3. **`trg_donation_ai` (AFTER INSERT on `donation`):** If a donation is inserted directly with status `'Pass'`, it automatically adds units to `blood_stock`.
4. **`trg_donation_bu` (BEFORE UPDATE on `donation`):** Locks core donation attributes (donor, bank, units, date, Hb). Enforces the screening state machine: once a donation transitions from `'Pending'` to `'Pass'` or `'Fail'`, the result is permanent and cannot be modified. It also ensures units can only be consumed, never added back.
5. **`trg_donation_au` (AFTER UPDATE on `donation`):** Synchronizes inventory with the database. When a donation changes from `'Pending'` to `'Pass'`, it increments `blood_stock`. When units are issued or expired, it decrements `blood_stock`.
6. **`trg_donation_bd` (BEFORE DELETE on `donation`):** Prohibits deleting any donation with screening result `'Pass'` because passed units back physical stock and issue audit logs.
7. **`trg_request_bi` (BEFORE INSERT on `blood_request`):** Enforces that every new blood request must start with status `'Pending'`.
8. **`trg_request_bu` (BEFORE UPDATE on `blood_request`):** Prevents modifying hospital, bank, blood group, or units once raised. Ensures that when a request status becomes `'Fulfilled'`, exactly the requested number of units have been logged in `request_issue`.
9. **`trg_issue_bi` (BEFORE INSERT on `request_issue`):** Validates cross-table business rules: verifying staff must work at the processing bank, blood group must match, donation screening must be `'Pass'`, verified date must be between request date and today, and total issued units cannot exceed the requested amount.
10. **`trg_issue_bu` & `trg_issue_bd` (UPDATE/DELETE on `request_issue`):** Makes issue audit records completely immutable and undeletable.

---

### Q3: How is the "double-issue" race condition prevented during request fulfilment?
**Answer:**
Double-issuing is prevented through **strict hierarchical row-level pessimistic locking (`SELECT ... FOR UPDATE`)**:
1. When `sp_fulfil_request` begins, it starts an explicit transaction (`START TRANSACTION`).
2. It first locks the `blood_request` row (`FOR UPDATE`), verifying its status is `'Pending'`.
3. It then acquires an exclusive lock on the corresponding `blood_stock` row for that `(bank_id, blood_group)`.
4. Finally, individual matching `donation` rows are locked in order of expiry date.
Because all sessions acquire locks in the exact same global order (`blood_request` $\rightarrow$ `blood_stock` $\rightarrow$ `donation`), deadlocks are mathematically eliminated and concurrent attempts to fulfil the same request or claim the same blood units are serialized by the InnoDB lock manager.

---

### Q4: What is FEFO and how does the application implement it?
**Answer:**
**FEFO** stands for **First-Expired, First-Out**. Because donated blood has a strictly limited biological shelf-life (35 days from collection), blood banks must issue the oldest units first to minimize wastage.
In procedure `sp_fulfil_request`, when units are allocated, the query orders viable donations by `expiry_date ASC, donation_id ASC LIMIT 1 FOR UPDATE`. This ensures units expiring soonest are consumed first before newer donations are touched.

---

### Q5: Why must `PreparedStatement` and `CallableStatement` be used instead of concatenated SQL queries?
**Answer:**
1. **Security:** `PreparedStatement` parameterizes variables, sending SQL logic and user data in separate protocol packets, completely preventing **SQL Injection attacks**.
2. **Performance:** Prepared statements are pre-compiled and parsed once by the MySQL server query engine; subsequent executions reuse the execution plan with different parameters.
3. **Correct Type Handling:** Handles conversions for dates, times, decimals, and nulls automatically without tedious quoting or locale date string issues.

---

### Q6: What is the difference between an Abstract Class and an Interface, and where is each used in your project?
**Answer:**
- **Abstract Class:** Can maintain state (instance variables), constructor logic, and provide partial implementation of common methods. Subclasses can only extend one abstract class (single inheritance).
  - *Used in:*
    - `User` (subclasses `AdminUser`, `BankStaffUser`, `HospitalUser`)
    - `Person` (subclasses `Donor`, `StaffMember`)
    - `BasePanel` (extended by all 11 Swing UI tab panels)
    - `BaseDao` (extended by all Data Access Objects)
- **Interface:** Defines a pure contract/behavior without instance state. A class can implement multiple interfaces.
  - *Used in:* `StockListener` (implemented by `StockPanel` to receive event notifications from the background `StockMonitor` thread).

---

### Q7: How does `MainFrame` demonstrate Polymorphism?
**Answer:**
In `MainFrame.buildRoleTabs()`, the code does not use `if (user instanceof AdminUser)` checks to decide what tabs to show. Instead, it calls the polymorphic method `currentUser.getTabs()`. Because `AdminUser`, `BankStaffUser`, and `HospitalUser` each override `getTabs()`, Java dynamic method dispatch invokes the subclass-specific implementation at runtime. If a new user role (such as `AuditorUser`) is added in the future, `MainFrame` works without modification (Open/Closed Principle).

---

### Q8: Why must UI updates inside background threads be wrapped in `SwingUtilities.invokeLater()`?
**Answer:**
Swing components are **not thread-safe**. All GUI manipulations, component additions, and model updates must strictly occur on the single **Event Dispatch Thread (EDT)**. If a background thread (such as `StockMonitor` or `ScheduledExecutorService`) modifies Swing components directly, it can trigger race conditions, repaint glitches, or internal exceptions. `SwingUtilities.invokeLater(runnable)` posts the task to the EDT event queue, ensuring safe execution.

---

### Q9: What is the difference between `Thread` and `SwingWorker`?
**Answer:**
- A raw `Thread` or `ScheduledExecutorService` is suited for long-running, periodic background tasks (such as `StockMonitor` polling stock levels every 10 seconds).
- `SwingWorker` is designed specifically for Swing GUI applications. It runs slow background operations (like database queries) on a worker thread in `doInBackground()`, and provides a `done()` lifecycle callback that automatically executes on the EDT when the background work finishes, making it easy to update the UI without manual thread synchronization.

---

### Q10: Why are custom checked exceptions used instead of catching raw `SQLException` in the UI?
**Answer:**
Raw `SQLException`s expose low-level technical internals (table names, SQL syntax, foreign key constraint names, stack traces) that are confusing and potentially insecure if displayed to end-users.
The application defines a custom hierarchy:
$$\text{AppException} \longrightarrow \{\text{ValidationException}, \text{DatabaseException}, \text{AuthenticationException}\}$$
DAOs catch `SQLException`, pass it to `ErrorMapper.toFriendly(e)` to translate error codes (e.g. 1062 duplicate, 1451 foreign key in use, 3819 check constraint violation) into friendly human-readable text, and wrap it into a `DatabaseException`. The UI catches `AppException` and displays `e.getMessage()` safely in a `JOptionPane`.

---

### Q11: Why does `DBConnection.getConnection()` return a new connection per operation instead of using a single global shared connection?
**Answer:**
MySQL connections are stateful and not thread-safe. If multiple concurrent threads (the UI thread, worker threads in `SwingWorker`, and the background `StockMonitor` thread) share one connection, their queries, transactions, and result sets would collide.
Returning a new connection per operation—used in a **try-with-resources** block—ensures that each thread has its own isolated session and transactions, and that connections are automatically closed even if an exception occurs.

---

### Q12: How are user passwords hashed, and why is a unique salt mandatory?
**Answer:**
Passwords are hashed using **SHA-256** on the combination of a randomly generated **16-hexadecimal character salt** and the plaintext password:
$$\text{Hash} = \text{SHA-256}(\text{salt} + \text{password})$$
- **Why Salt?** If two users choose the same password (e.g. `"password123"`), without a salt their hashes would be identical, and attackers could use precomputed **Rainbow Tables** to crack passwords instantly. A unique random salt ensures that every hash is unique even for identical passwords.
- **Verification:** During login, `PasswordUtil.verifyPassword()` uses `MessageDigest.isEqual()` to compare byte arrays in constant time, preventing timing attacks.

---

### Q13: What does the MySQL Event Scheduler do in this system?
**Answer:**
File `db/04_triggers.sql` defines the event `ev_expire_stock`:
```sql
CREATE EVENT ev_expire_stock
  ON SCHEDULE EVERY 1 DAY STARTS (TIMESTAMP(CURRENT_DATE) + INTERVAL 1 DAY + INTERVAL 5 MINUTE)
  DO CALL sp_expire_all();
```
Every night at 00:05 AM, the MySQL internal daemon runs `sp_expire_all()`, zeroing `units_remaining` for any donation that reached its 35-day expiry date and deducting them from stock via `trg_donation_au`.
*(In addition, whenever `sp_fulfil_request` runs, it calls `sp_expire_pair` first, ensuring expired units are never issued even before the daily sweep runs).*

---

### Q14: Why is a screening result of `Pass` or `Fail` irreversible?
**Answer:**
Medical blood testing protocols require strict non-repudiation. A blood unit that has tested positive for an infectious disease (`Fail`) must never be remarked as safe. Conversely, a unit marked `Pass` is immediately published to available inventory, can be locked for hospital requests, or issued to patients. Reversing a `Pass` would invalidate historical issue audit trails (`request_issue`). Trigger `trg_donation_bu` rejects any modification once the status is not `'Pending'`.

---

### Q15: Why is there no `ON DELETE CASCADE` on `app_user` foreign keys?
**Answer:**
In `db/05_app_user.sql`, the table defines a `CHECK` constraint:
```sql
CONSTRAINT ck_user_link CHECK (
     (role='ADMIN'    AND staff_id IS NULL     AND hospital_id IS NULL)
  OR (role='STAFF'    AND staff_id IS NOT NULL AND hospital_id IS NULL)
  OR (role='HOSPITAL' AND hospital_id IS NOT NULL AND staff_id IS NULL))
```
MySQL forbids `ON DELETE CASCADE` or `ON DELETE SET NULL` referential actions on foreign key columns that participate in a SQL `CHECK` constraint (to prevent automatic cascaded nullifications from violating the check constraint). Therefore, the application code in `StaffDao.deleteStaff()` and `HospitalDao.deleteHospital()` explicitly deletes the corresponding `app_user` record *before* deleting the staff or hospital entity.

---

### Q16: How does the application use the Java Collections framework?
**Answer:**
1. **`List<T>` / `ArrayList<T>`:** Used throughout DAOs and UI models for populating `JTable` rows.
2. **`Map<String, Integer>` (`LinkedHashMap`):** Used in `StockDao.getStockForBank()` and `StockMonitor` to map blood groups or bank-group pairs to available unit counts.
3. **`Set<String>` (`HashSet`):** Used in `StockMonitor` to store already-alerted low-stock pairs, preventing warning dialogs from repeating every 10 seconds until stock recovers.
4. **`Comparator<BloodRequest>`:** Implemented as `BloodRequest.BY_URGENCY_THEN_DATE`, sorting pending requests by urgency (Critical $\rightarrow$ Urgent $\rightarrow$ Normal) and then by oldest request timestamp.
5. **`Collections.unmodifiableList()`:** Used in `Person.getPhones()` to protect internal phone lists from external mutation.

---

### Q17: How does the low stock threshold warning system work?
**Answer:**
`AppConfig` reads `stock.low.threshold` (default: 5 units).
The `StockMonitor` background thread executes periodically. When it finds any `(bank, group)` where `units_available < threshold`:
- If the pair is not already in the `alreadyAlertedPairs` set, it generates a warning and adds it to the set.
- If a previously low group receives new donations and rises $\ge 5$, it is removed from `alreadyAlertedPairs`, so if it drops again in the future, a fresh alert will trigger.
- The UI highlights all deficit rows in **soft red background with bold text** via a custom `TableCellRenderer` in `StockPanel.java`.

---

### Q18: What is `demo.hold.seconds`, and how can you demonstrate transaction row-locking?
**Answer:**
In `config/db.properties`, `demo.hold.seconds` defaults to 0. When set to a positive integer (e.g. 15):
1. `sp_fulfil_request` executes `DO SLEEP(p_hold_sec)` after locking the request and stock rows.
2. The student can open **two separate application instances** simultaneously.
3. Both staff attempt to fulfil the same pending blood request.
4. The first session immediately acquires the lock and enters the sleep.
5. The second session attempting fulfilment is paused waiting for the InnoDB lock.
6. When the first session commits (`status = 'Fulfilled'`), the lock is released; the second session resumes, immediately detects `status <> 'Pending'`, rolls back, and receives the error `"Request is already processed"`.
This visibly demonstrates ACID concurrency isolation and row-level locking during viva evaluation.

---

### Q19: Why does `sp_fulfil_request` return `'Rejected'` as an OUT parameter instead of throwing an exception when stock is short?
**Answer:**
In business terms, having insufficient stock is a **valid business outcome**, not a database failure or corrupt state. If it threw an unhandled SQL exception, the request would remain `Pending` and could repeatedly block the queue. By updating `status = 'Rejected'`, committing, and returning `'Rejected'`, the request is formally closed, allowing the hospital to seek units from an alternative bank without hanging the transaction.

---

### Q20: What are the primary strengths of this architecture for viva evaluation?
**Answer:**
1. **True Separation of Concerns:** Database owns data rules; Java owns user experience and presentation.
2. **Defensive Programming:** Parameterized SQL, transaction safety, try-with-resources, custom exceptions, and unmodifiable collections.
3. **Multithreaded Responsiveness:** Background scheduling for monitoring, `SwingWorker` for async I/O, EDT safety.
4. **Clean OOP Design:** Clean inheritance hierarchy with true polymorphism (`getTabs()`).
