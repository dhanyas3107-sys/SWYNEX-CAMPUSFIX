-- ============================================================================
-- CampusFix - OPTIONAL demo data. Run AFTER schema.sql:
--     mysql -u root -p campusfix < database/sample_data.sql
--
-- SECURITY NOTE: no real password exists in this file. Every demo account has the
-- placeholder hash 'LOGIN_DISABLED', which can never match any password, so nobody can
-- log in as a demo user. The demo administrator is additionally marked inactive, so it does
-- not block the one-time --init-admin setup. The demo technicians are active, so you can
-- assign complaints to them and see workloads in the reports. Create real accounts through the app:
--   * administrator:  run the app with --init-admin
--   * students/faculty: menu options 1 and 2
--   * technicians: Administrator -> Manage Technicians -> Add technician
--
-- Re-running this script on a database that already contains it will fail on duplicate
-- ids (that is intentional - it protects existing data). Use a fresh database.
-- ============================================================================

USE campusfix;

INSERT INTO users (user_id, name, email, password_hash, phone_number, role, is_active, created_at) VALUES
 (1, 'Demo Admin (disabled)',  'demo.admin@campusfix.example',   'LOGIN_DISABLED', '9000000001', 'ADMINISTRATOR', 0, '2026-11-01 09:00:00'),
 (2, 'Aarav Sharma',           'aarav.sharma@campusfix.example', 'LOGIN_DISABLED', '9000000002', 'STUDENT',       1, '2026-11-02 09:00:00'),
 (3, 'Meera Iyer',             'meera.iyer@campusfix.example',   'LOGIN_DISABLED', '9000000003', 'STUDENT',       1, '2026-11-02 09:30:00'),
 (4, 'Dr. Ramesh Rao',         'ramesh.rao@campusfix.example',   'LOGIN_DISABLED', '9000000004', 'FACULTY',       1, '2026-11-02 10:00:00'),
 (5, 'Ravi Kumar',             'ravi.kumar@campusfix.example',   'LOGIN_DISABLED', '9000000005', 'TECHNICIAN',    1, '2026-11-01 10:00:00'),
 (6, 'Suresh Babu',            'suresh.babu@campusfix.example',  'LOGIN_DISABLED', '9000000006', 'TECHNICIAN',    1, '2026-11-01 10:05:00'),
 (7, 'Lakshmi Narayanan',      'lakshmi.n@campusfix.example',    'LOGIN_DISABLED', '9000000007', 'TECHNICIAN',    1, '2026-11-01 10:10:00');

INSERT INTO technicians (technician_id, user_id, specialization, contact_info, availability) VALUES
 (1, 5, 'ELECTRICAL', 'Maintenance office, ext. 101', 'AVAILABLE'),
 (2, 6, 'NETWORK',    'IT help desk, ext. 202',       'BUSY'),
 (3, 7, 'PLUMBING',   'Maintenance office, ext. 103', 'AVAILABLE');

INSERT INTO complaints
 (complaint_id, submitted_by, category, description, building, location, priority, status,
  assigned_technician_id, created_at, resolved_at) VALUES
 (1, 2, 'ELECTRICAL',    'Two tube lights in the classroom flicker and sometimes switch off.',     'Main Block',   'Room 204',       'MEDIUM',   'SUBMITTED',    NULL, '2026-11-10 09:15:00', NULL),
 (2, 2, 'NETWORK',       'Wi-Fi keeps disconnecting on the second floor corridor every few minutes.', 'Library',  'Second floor',   'HIGH',     'ASSIGNED',     2,    '2026-11-11 11:00:00', NULL),
 (3, 4, 'PLUMBING',      'Tap in the faculty washroom is leaking continuously.',                   'Science Block','Ground floor',   'HIGH',     'IN_PROGRESS',  3,    '2026-11-12 08:45:00', NULL),
 (4, 3, 'ELECTRICAL',    'Power socket near the lab bench 5 has burnt marks and sparks.',          'Lab Block',    'Electronics Lab','CRITICAL', 'RESOLVED',     1,    '2026-11-05 10:00:00', '2026-11-05 15:30:00'),
 (5, 3, 'CLEANLINESS',   'Classroom floor has not been cleaned for several days.',                 'Main Block',   'Room 110',       'LOW',      'CANCELLED',    NULL, '2026-11-13 09:00:00', NULL),
 (6, 4, 'FURNITURE',     'Request to replace all chairs in the seminar hall with new ones.',       'Admin Block',  'Seminar Hall',   'LOW',      'REJECTED',     NULL, '2026-11-14 12:00:00', NULL);

INSERT INTO complaint_assignments
 (assignment_id, complaint_id, technician_id, assigned_by, assigned_at, accepted_at, released_at, completed_at, is_active) VALUES
 (1, 2, 2, 1, '2026-11-11 12:00:00', NULL,                  NULL, NULL,                  1),
 (2, 3, 3, 1, '2026-11-12 09:30:00', '2026-11-12 10:00:00', NULL, NULL,                  1),
 (3, 4, 1, 1, '2026-11-05 10:30:00', '2026-11-05 11:00:00', NULL, '2026-11-05 15:30:00', 0);

INSERT INTO complaint_history (complaint_id, previous_status, new_status, updated_by, changed_at, remarks) VALUES
 (1, NULL,           'SUBMITTED',    2, '2026-11-10 09:15:00', 'Complaint submitted'),
 (2, NULL,           'SUBMITTED',    2, '2026-11-11 11:00:00', 'Complaint submitted'),
 (2, 'SUBMITTED',    'UNDER_REVIEW', 1, '2026-11-11 11:55:00', 'Moved to review for assignment'),
 (2, 'UNDER_REVIEW', 'ASSIGNED',     1, '2026-11-11 12:00:00', 'Assigned to Suresh Babu'),
 (3, NULL,           'SUBMITTED',    4, '2026-11-12 08:45:00', 'Complaint submitted'),
 (3, 'SUBMITTED',    'UNDER_REVIEW', 1, '2026-11-12 09:25:00', 'Moved to review for assignment'),
 (3, 'UNDER_REVIEW', 'ASSIGNED',     1, '2026-11-12 09:30:00', 'Assigned to Lakshmi Narayanan'),
 (3, 'ASSIGNED',     'IN_PROGRESS',  7, '2026-11-12 10:00:00', 'Assignment accepted; work started'),
 (3, 'IN_PROGRESS',  'IN_PROGRESS',  7, '2026-11-12 14:00:00', 'Progress note: washer replacement part ordered'),
 (4, NULL,           'SUBMITTED',    3, '2026-11-05 10:00:00', 'Complaint submitted'),
 (4, 'SUBMITTED',    'UNDER_REVIEW', 1, '2026-11-05 10:25:00', 'Moved to review for assignment'),
 (4, 'UNDER_REVIEW', 'ASSIGNED',     1, '2026-11-05 10:30:00', 'Assigned to Ravi Kumar'),
 (4, 'ASSIGNED',     'IN_PROGRESS',  5, '2026-11-05 11:00:00', 'Assignment accepted; work started'),
 (4, 'IN_PROGRESS',  'RESOLVED',     5, '2026-11-05 15:30:00', 'Resolved: socket replaced and wiring tested'),
 (5, NULL,           'SUBMITTED',    3, '2026-11-13 09:00:00', 'Complaint submitted'),
 (5, 'SUBMITTED',    'CANCELLED',    3, '2026-11-13 09:20:00', 'Cancelled by the submitter: cleaned already'),
 (6, NULL,           'SUBMITTED',    4, '2026-11-14 12:00:00', 'Complaint submitted'),
 (6, 'SUBMITTED',    'REJECTED',     1, '2026-11-14 16:00:00', 'Rejected: this is a purchase request, not a maintenance problem');
