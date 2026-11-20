# CampusFix – Campus Maintenance and Complaint Management System

A menu-driven **Java console application** with a **MySQL** database where students and faculty report
campus infrastructure problems, administrators assign technicians and track progress, and technicians
resolve the work. Built with core Java, JDBC, Maven and JUnit 5 – no frameworks.

---

## 1. Problem statement

In many colleges a broken projector, a leaking tap or a dead Wi-Fi router is reported verbally or on paper.
Nobody can see who is responsible, how long the problem has been open, or whether it was really fixed.

## 2. Objectives

* Give every complaint a unique ID, a lifecycle and a complete audit trail.
* Make sure the right person (administrator / technician / reporter) – and only that person – can act on it.
* Show OOP, JDBC, validation, exception handling, transactions, testing and clean layering in one project.

## 3. Features

* Registration and login for students and faculty; salted PBKDF2 password hashing.
* Role-based menus **and** role-based enforcement inside the service layer.
* Submit, view, track, edit (while still `SUBMITTED`) and cancel (before assignment) complaints.
* Strict status workflow – invalid transitions are rejected in Java and the terminal states are protected in MySQL.
* Administrator: view / search / filter complaints, review or reject, assign and reassign technicians,
  change priority (justification required), manage technicians, reports.
* Rule-based technician recommendation (specialization → availability → current workload).
* Technician: view own assignments, accept, add progress notes, resolve (notes required), view completed tasks.
* Append-only complaint history; a status change and its history row are saved in **one transaction**.
* SQL aggregate reports (`COUNT`, `GROUP BY`, `AVG`, `ORDER BY`).

## 4. User roles

| Role | What they can do |
|---|---|
| **Student / Faculty** | Register, log in, submit, view own, track, view history, edit (only `SUBMITTED`), cancel (only before assignment) |
| **Administrator** | View all, search, filter, review/reject, assign/reassign technicians, change priority, manage technicians, reports, view any history |
| **Technician** | View own assignments, accept, update status (`IN_PROGRESS` / `RESOLVED`), add notes, view completed tasks. Cannot touch other technicians' complaints |

## 5. Technologies

Java 17+ · MySQL 8 (8.0.16+ so `CHECK` constraints are enforced) · JDBC (MySQL Connector/J 8.4.0) · Maven · JUnit 5.10 · Git/GitHub · VS Code.

## 6. System architecture

```
 Console (ui/ + Main)  ──►  Services (business rules, RBAC, transactions)
                               │              │
                               ▼              ▼
                         Validators         DAOs (JDBC + PreparedStatement)
                                              │
                                              ▼
                                   DatabaseConnection ──► MySQL
 Model (entities, enums) and Exceptions are used by every layer.
```

| Layer | Responsibility |
|---|---|
| `model` | Entities (`User` + subclasses, `Complaint`, `ComplaintHistory`) and enums (`ComplaintStatus` owns the workflow rules) |
| `dao` | Only SQL. Every method takes a `Connection`, so a service can run several DAO calls in one transaction |
| `service` | Business logic, authorization (`AuthorizationService`), transactions, recommendation algorithm |
| `validation` | Input checks (email, phone, password, complaint text) |
| `database` | `DatabaseConnection`: reads credentials from environment variables; `read()` / `transaction()` helpers close connections with try-with-resources and commit/rollback |
| `ui` + `Main` | Menus and console formatting only |
| `exception` | Custom unchecked exceptions; the UI catches them once and prints a friendly message |

## 7. Project structure

```
campusfix/
├── pom.xml
├── run.sh                      # loads .env, compiles, runs
├── .env.example                # template for credentials (copy to .env)
├── .gitignore
├── README.md
├── database/
│   ├── schema.sql              # tables, constraints, indexes, triggers
│   └── sample_data.sql         # optional demo data (no usable passwords)
└── src/
    ├── main/java/com/campusfix/
    │   ├── Main.java, AppContext.java
    │   ├── model/       User, Student, Faculty, Administrator, Technician, Complaint, ComplaintHistory,
    │   │                enums (Role, Category, Priority, ComplaintStatus, Availability, Permission), report records
    │   ├── dao/         UserDAO, ComplaintDAO, TechnicianDAO, ComplaintHistoryDAO, ReportDAO
    │   ├── service/     AuthenticationService, AuthorizationService, UserService, ComplaintService,
    │   │                TechnicianService, ReportService
    │   ├── database/    DatabaseConnection
    │   ├── validation/  InputValidator, UserValidator, ComplaintValidator
    │   ├── exception/   CampusFixException (base) + 6 specific exceptions
    │   ├── util/        PasswordUtil, IdGenerator, ConsoleUtil
    │   └── ui/          SubmitterMenu, AdminMenu, TechnicianMenu, ComplaintPrinter
    └── test/java/com/campusfix/   unit tests + one opt-in integration test (CampusFixWorkflowIT)
```

## 8. Database schema and relationships

Five tables: `users`, `technicians`, `complaints`, `complaint_assignments`, `complaint_history`.

```
users 1 ───── 0..1 technicians          (a technician is a user with role TECHNICIAN)
users 1 ───── * complaints              (submitted_by)
technicians 1 ─ * complaints            (assigned_technician_id, nullable)
complaints 1 ── * complaint_assignments (every assignment ever made)
technicians 1 ─ * complaint_assignments
users 1 ───── * complaint_assignments   (assigned_by = the administrator)
complaints 1 ── * complaint_history
users 1 ───── * complaint_history       (updated_by)
```

Integrity rules enforced **in the database** (not only in Java):

* `UNIQUE(email)`; `CHECK` on email shape, 10–15 digit phone, non-blank text.
* All relationships are foreign keys; statuses/priorities/categories/roles are `ENUM`s.
* `CHECK`: a complaint that is `ASSIGNED`, `IN_PROGRESS` or `RESOLVED` must have a technician; `resolved_at` is set **iff** the status is `RESOLVED`.
* A generated column + `UNIQUE` index allows only **one active assignment per complaint**.
* Trigger: `RESOLVED`, `REJECTED`, `CANCELLED` can never change status again.
* Triggers: `complaint_history` rows cannot be updated or deleted.
* Indexes on status, category, priority, submitter, technician.

### Status workflow

```
SUBMITTED ─► UNDER_REVIEW ─► ASSIGNED ─► IN_PROGRESS ─► RESOLVED
   │  │            │  │          ▲             │
   │  └► REJECTED ◄┘  └► CANCELLED   └─────────┘  (reassignment only)
   └────► CANCELLED
```
`RESOLVED`, `REJECTED`, `CANCELLED` are final. Assigning a `SUBMITTED` complaint passes through `UNDER_REVIEW`
automatically (two history rows). Only the assigned technician can move a complaint to `RESOLVED`.

### Technician recommendation (rule based – not AI)

1. Exclude technicians who are disabled, `ON_LEAVE`, or already hold 8 active assignments.
2. Sort the rest by: specialization matches the category → `AVAILABLE` before `BUSY` → fewest active assignments → lowest technician id.
3. The first one is the recommendation; the screen shows the reason for each. The admin can accept it (press Enter) or pick another technician.

(Documented in the Javadoc of `TechnicianService`, tested in `TechnicianAssignmentTest`.)

## 9. Installation requirements (macOS)

```bash
brew install openjdk@17 maven mysql
# make Java 17 visible (Homebrew prints the exact line; this is the usual one):
sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk
brew services start mysql
java -version && mvn -version        # both must work; Java must be 17 or higher
```
In VS Code install the **Extension Pack for Java**, then *File → Open Folder → campusfix*.

## 10. MySQL setup

```bash
# 1. create the database, tables, constraints and triggers
mysql -u root -p < database/schema.sql

# 2. create a limited account for the application (choose your own strong password)
mysql -u root -p -e "CREATE USER 'campusfix_app'@'localhost' IDENTIFIED BY 'choose-a-strong-password'; \
                     GRANT SELECT, INSERT, UPDATE ON campusfix.* TO 'campusfix_app'@'localhost';"

# 3. (optional) load demo data into a fresh database
mysql -u root -p campusfix < database/sample_data.sql
```
`sample_data.sql` contains no usable passwords (see the comment at the top of that file).

## 11. Environment variables

The app never contains credentials. It reads:

| Variable | Required | Meaning |
|---|---|---|
| `CAMPUSFIX_DB_URL` | no | default `jdbc:mysql://localhost:3306/campusfix?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `CAMPUSFIX_DB_USER` | **yes** | MySQL user (e.g. `campusfix_app`) |
| `CAMPUSFIX_DB_PASSWORD` | **yes** | that user's password |
| `CAMPUSFIX_DEBUG` | no | `true` prints stack traces for unexpected errors |

```bash
cp .env.example .env     # then edit .env – it is git-ignored
```
`run.sh` loads `.env` automatically. To set variables manually in a terminal instead:
`export CAMPUSFIX_DB_USER=campusfix_app` and `export CAMPUSFIX_DB_PASSWORD='...'`.

## 12. Build

```bash
mvn clean compile        # compile only
mvn clean test           # compile + run unit tests (no database needed)
mvn clean package        # compile + test + jar (target/campusfix-1.0.0.jar, without dependencies)
```

## 13. Run

```bash
./run.sh --init-admin    # ONE TIME: creates the first administrator
./run.sh                 # start the application
```
If `./run.sh` says permission denied: `chmod +x run.sh`.

Fallback without the script (password typing is then visible, because Maven hides the terminal):
```bash
export CAMPUSFIX_DB_USER=... CAMPUSFIX_DB_PASSWORD=...
mvn -q compile exec:java                           # run
mvn -q compile exec:java -Dexec.args="--init-admin" # first administrator
```

### Secure first-administrator setup
There is **no default admin and no default password**. `--init-admin` asks you for name, e-mail, phone and a password
(validated, hashed with PBKDF2 and a unique salt, never printed). It refuses to run again once an active administrator exists,
so it cannot be used later to create hidden admins. Further technicians are created by the administrator
(**Manage Technicians → Add technician**).

## 14. Sample workflow

1. `./run.sh --init-admin` → create the administrator.
2. `./run.sh` → **1. Student Registration** → register a student; log in; **Submit Complaint** → note the ID, e.g. `CF-000001`.
3. Log in as the administrator → **Manage Technicians → Add technician** (e.g. ELECTRICAL).
4. **Assign Technician** → enter the complaint ID → press Enter to accept the recommendation. Status becomes `ASSIGNED`.
5. Log in as the technician → **Accept Assignment** → **Add Progress Notes** → **Update Complaint Status → RESOLVED** (notes required).
6. Log in as the student → **Track Complaint** and **View Complaint History** show every step with who/when.
7. Administrator → **Generate Reports**.

Menus follow the specification. Two additions were needed to make required features reachable:
students/faculty have **6. Edit Complaint** (Logout is 7), and administrators have **9. Review / Reject Complaint** (Logout is 10).

## 15. Testing

```bash
mvn clean test
```
59 unit tests (no database required) cover: email, phone, password and name validation; user and complaint validation;
status-transition rules; technician assignment rules and ranking; PBKDF2 hashing/verification (salt uniqueness, wrong password,
malformed hashes); role-based authorization and polymorphic roles; complaint-ID formatting/parsing.

### Integration test (needs MySQL)
`CampusFixWorkflowIT` runs the whole lifecycle (register → submit → assign → accept → note → priority change → resolve →
history → report, plus negative cases) against a real database. It is skipped by `mvn clean test`.
Use a **separate throw-away database**:

```bash
sed 's/campusfix/campusfix_test/g' database/schema.sql | mysql -u root -p
mysql -u root -p -e "GRANT SELECT, INSERT, UPDATE ON campusfix_test.* TO 'campusfix_app'@'localhost';"

export CAMPUSFIX_IT=true
export CAMPUSFIX_DB_URL="jdbc:mysql://localhost:3306/campusfix_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export CAMPUSFIX_DB_USER=campusfix_app
export CAMPUSFIX_DB_PASSWORD='...'
mvn test -Dtest=CampusFixWorkflowIT
```

## 16. Security features

* PBKDF2-HMAC-SHA256, 600 000 iterations, 16-byte random salt per password, constant-time comparison; hash string carries its own parameters.
* Passwords and hashes are never printed, logged or put in `toString()`.
* Generic login error (“Invalid email or password.”) for unknown user, wrong password and disabled account; a dummy hash check keeps timing similar.
* Role-based access control in `AuthorizationService`, checked by every service method (not only hidden in menus); ownership checks for users and technicians.
* 100 % `PreparedStatement` – dynamic filters only append fixed `?` fragments; `LIKE` wildcards in search text are escaped.
* Credentials only from environment variables; `.env` is git-ignored; the application database user needs no `DELETE`/`DDL` rights.
* Database enforces uniqueness, foreign keys, `CHECK`s, terminal states and append-only history.
* Compare-and-set status updates + row locks (`SELECT … FOR UPDATE`) stop two people changing one complaint at once.
* Errors shown to users are friendly; SQL details stay in the exception cause (visible only with `CAMPUSFIX_DEBUG=true`).

## 17. Known limitations / future enhancements

* No login attempt lockout or password reset / change feature.
* Passwords travel through Java `String`s (immutable); a hardened version would use `char[]` end to end.
* No e-mail/SMS notifications, attachments/photos, SLA timers or web/mobile UI.
* No connection pool (a new connection per operation; fine for a console app, a pool such as HikariCP would be the next step).
* Pagination for very large complaint lists.


## 19. Author and internship context

Built as the final task of a **Java Developer Internship** by a second-year Computer Science Engineering student, to
demonstrate OOP, JDBC/MySQL, validation, exception handling, clean architecture, testing and documentation.

*Author:* DHANYA · *Institution:* CHENNAI INSTITUTE OF TECHNOLOGY · *Internship provider: SWYNEX TECHNOLOGY
