# BRIEF.md — Blood Bank & Donation Management System (Java Swing + JDBC)

> **For Antigravity:** read this whole file first, then build **phase by phase** (Section 12). After each phase: compile, run, fix, then `git commit`. Do not skip ahead. Ask the user only when something marked **ASK** is missing.

---

## 1. What you are building

A desktop app (Java Swing) that is the front end for an **already finished MySQL backend** (`blood_bank_db_er`). The database enforces all business rules through stored procedures and triggers. The Java app **calls them and shows their results**; it must never re-implement or bypass those rules.

Life of a unit of blood: donor registers → donates (Pending) → screened (Pass/Fail) → Pass adds to bank stock → hospital raises request → bank staff fulfil (earliest-expiring units first) → expired units swept daily.

This is a **college mini project graded on a 10-mark rubric**, so the code must visibly demonstrate each rubric item (Section 2). Keep it clean, readable and explainable in a viva. Prefer simple, well-commented code over clever code.

**Hard rules**
- Do **not** edit `db/01`–`db/04` SQL files. They are frozen. If you believe you found a backend bug, report it to the user instead of changing it.
- Use only the Java standard library + MySQL Connector/J. No Spring, no Hibernate, no Lombok.
- Java 17. Swing only (no JavaFX).
- All SQL uses `PreparedStatement` / `CallableStatement` (no string-concatenated SQL).
- Never commit real DB credentials.

---

## 2. Rubric → what must exist (10 marks)

| Criteria | Marks | What the grader must be able to see |
|---|---|---|
| Problem analysis & design | 1 | `docs/DESIGN.md`: problem statement, requirements, roles, ER/flow summary, class diagram (Mermaid is fine), and the 8-step workflow |
| **Inheritance** | 1 | `User` (abstract) → `AdminUser`, `BankStaffUser`, `HospitalUser`, each overriding `getTabs()`; `Person` (abstract) → `Donor`, `StaffMember`; `BasePanel` (abstract, extends `JPanel`) → every tab; `BaseDao` (abstract) → every DAO. Use polymorphism: `MainFrame` builds tabs by calling `user.getTabs()` |
| **Collections** | 1 | `List<>`/`ArrayList` for table rows, `Map<String,Integer>` (`HashMap`/`LinkedHashMap`) for stock per blood group, `Set<String>` for already-alerted groups, `Comparator` sorting of requests by urgency then date, `Collections.unmodifiableList` on model getters |
| **Exception handling** | 1 | Custom exceptions (`AppException` base → `ValidationException`, `DatabaseException`, `AuthenticationException`); `ErrorMapper` turns `SQLException` into friendly text; try-with-resources everywhere; no empty `catch` blocks; no stack traces shown to the user |
| **JDBC CRUD** | 2 | Create / Read / Update / Delete all demonstrated with `PreparedStatement` (see Section 6 CRUD matrix) plus `CallableStatement` for stored procedures |
| **GUI** | 1 | Login window + role-based tabbed main window, tables (`JTable`), forms, dialogs, colour-coded stock, status bar |
| **Thread** | 1 | `StockMonitor` background thread refreshing stock and raising warnings, updating UI only via `SwingUtilities.invokeLater`; `SwingWorker` for slow DB calls; clean shutdown on exit |
| Viva | 1 | `docs/VIVA_NOTES.md` (Section 11) so the student can answer design/implementation/Java-concept questions |

Add a short `docs/RUBRIC_MAPPING.md` that lists, for each row above, the exact file and class names that satisfy it.

---

## 3. Folder structure (keep it exactly this simple)

```
BloodBankApp/
├── BRIEF.md                    <- this file
├── README.md                   <- setup + run + demo logins
├── .gitignore
├── build.sh / build.bat        <- javac into out/, run with java -cp
├── config/
│   ├── db.properties.example   <- committed
│   └── db.properties           <- NOT committed (user's real credentials)
├── db/
│   ├── 01_blood_bank_schema.sql
│   ├── 02_functions.sql
│   ├── 03_procedures.sql
│   ├── 04_triggers.sql
│   ├── 05_app_user.sql         <- you create (Section 4)
│   └── 06_seed_data.sql        <- you create (Section 4)
├── lib/
│   └── mysql-connector-j-8.x.x.jar
├── docs/
│   ├── DESIGN.md
│   ├── RUBRIC_MAPPING.md
│   ├── VIVA_NOTES.md
│   └── screenshots/
├── src/com/bloodbank/
│   ├── Main.java
│   ├── exception/   AppException, ValidationException, DatabaseException, AuthenticationException
│   ├── model/       Person, Donor, StaffMember, User, AdminUser, BankStaffUser, HospitalUser,
│   │                Bank, Hospital, Donation, BloodRequest, StockEntry
│   ├── dao/         BaseDao, AuthDao, BankDao, HospitalDao, StaffDao, DonorDao,
│   │                DonationDao, RequestDao, StockDao
│   ├── ui/          LoginFrame, MainFrame, BasePanel, and one panel per tab (Section 7)
│   ├── thread/      StockMonitor, StockListener
│   └── util/        DBConnection, AppConfig, PasswordUtil, ErrorMapper, Validator, Session
└── out/                        <- compiled classes, gitignored
```
