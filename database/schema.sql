-- ============================================================================
-- CampusFix - database schema (MySQL 8.0.16 or newer; CHECK constraints are
-- only enforced from 8.0.16).
--
-- Run with:   mysql -u root -p < database/schema.sql
-- Safe to run more than once: tables use IF NOT EXISTS, triggers are re-created.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS campusfix
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE campusfix;

-- ----------------------------------------------------------------------------
-- 1. users  (students, faculty, administrators and technician logins)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id        INT           NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100)  NOT NULL,
    email          VARCHAR(150)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,   -- PBKDF2 hash string, never the password
    phone_number   VARCHAR(20)   NOT NULL,
    role           ENUM('STUDENT', 'FACULTY', 'ADMINISTRATOR', 'TECHNICIAN') NOT NULL,
    is_active      TINYINT(1)    NOT NULL DEFAULT 1,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_name  CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_users_email CHECK (email LIKE '%_@_%.__%'),
    CONSTRAINT chk_users_phone CHECK (phone_number REGEXP '^[+]?[0-9]{10,15}$'),
    CONSTRAINT chk_users_active CHECK (is_active IN (0, 1)),
    INDEX idx_users_role (role)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- 2. technicians  (profile of users whose role is TECHNICIAN)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS technicians (
    technician_id  INT           NOT NULL AUTO_INCREMENT,
    user_id        INT           NOT NULL,
    specialization ENUM('ELECTRICAL', 'PLUMBING', 'NETWORK', 'CLEANLINESS',
                        'FURNITURE', 'LAB_EQUIPMENT', 'OTHER') NOT NULL,
    contact_info   VARCHAR(150)  NOT NULL,
    availability   ENUM('AVAILABLE', 'BUSY', 'ON_LEAVE') NOT NULL DEFAULT 'AVAILABLE',
    PRIMARY KEY (technician_id),
    CONSTRAINT uq_technicians_user UNIQUE (user_id),
    CONSTRAINT fk_technicians_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_technicians_contact CHECK (CHAR_LENGTH(TRIM(contact_info)) > 0),
    INDEX idx_technicians_specialization (specialization, availability)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- 3. complaints
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaints (
    complaint_id            INT           NOT NULL AUTO_INCREMENT,   -- shown to users as CF-000123
    submitted_by            INT           NOT NULL,
    category                ENUM('ELECTRICAL', 'PLUMBING', 'NETWORK', 'CLEANLINESS',
                                 'FURNITURE', 'LAB_EQUIPMENT', 'OTHER') NOT NULL,
    description             VARCHAR(1000) NOT NULL,
    building                VARCHAR(100)  NOT NULL,
    location                VARCHAR(100)  NOT NULL,
    priority                ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    status                  ENUM('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS',
                                 'RESOLVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'SUBMITTED',
    assigned_technician_id  INT           NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at             TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (complaint_id),
    CONSTRAINT fk_complaints_user       FOREIGN KEY (submitted_by)           REFERENCES users (user_id),
    CONSTRAINT fk_complaints_technician FOREIGN KEY (assigned_technician_id) REFERENCES technicians (technician_id),
    CONSTRAINT chk_complaints_description CHECK (CHAR_LENGTH(TRIM(description)) > 0),
    CONSTRAINT chk_complaints_building    CHECK (CHAR_LENGTH(TRIM(building)) > 0),
    CONSTRAINT chk_complaints_location    CHECK (CHAR_LENGTH(TRIM(location)) > 0),
    -- a complaint that is assigned / being worked on / resolved must have a technician
    CONSTRAINT chk_complaints_technician_required
        CHECK (status NOT IN ('ASSIGNED', 'IN_PROGRESS', 'RESOLVED') OR assigned_technician_id IS NOT NULL),
    -- resolved_at is set if and only if the complaint is RESOLVED
    CONSTRAINT chk_complaints_resolved_at
        CHECK ((status = 'RESOLVED' AND resolved_at IS NOT NULL)
            OR (status <> 'RESOLVED' AND resolved_at IS NULL)),
    INDEX idx_complaints_submitted_by (submitted_by, created_at),
    INDEX idx_complaints_status       (status),
    INDEX idx_complaints_category     (category),
    INDEX idx_complaints_priority     (priority),
    INDEX idx_complaints_technician   (assigned_technician_id, status)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- 4. complaint_assignments  (every assignment ever made, including reassignments)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint_assignments (
    assignment_id  INT        NOT NULL AUTO_INCREMENT,
    complaint_id   INT        NOT NULL,
    technician_id  INT        NOT NULL,
    assigned_by    INT        NOT NULL,                       -- the administrator
    assigned_at    TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at    TIMESTAMP  NULL DEFAULT NULL,
    released_at    TIMESTAMP  NULL DEFAULT NULL,              -- set when the complaint is reassigned
    completed_at   TIMESTAMP  NULL DEFAULT NULL,              -- set when the technician resolves it
    is_active      TINYINT(1) NOT NULL DEFAULT 1,
    -- equals complaint_id while the assignment is active, otherwise NULL.
    -- The UNIQUE index below therefore allows only ONE active assignment per complaint.
    active_complaint_key INT GENERATED ALWAYS AS (CASE WHEN is_active = 1 THEN complaint_id ELSE NULL END) STORED,
    PRIMARY KEY (assignment_id),
    CONSTRAINT uq_one_active_assignment UNIQUE (active_complaint_key),
    CONSTRAINT fk_assignments_complaint  FOREIGN KEY (complaint_id)  REFERENCES complaints (complaint_id),
    CONSTRAINT fk_assignments_technician FOREIGN KEY (technician_id) REFERENCES technicians (technician_id),
    CONSTRAINT fk_assignments_admin      FOREIGN KEY (assigned_by)   REFERENCES users (user_id),
    CONSTRAINT chk_assignments_active CHECK (is_active IN (0, 1)),
    INDEX idx_assignments_technician (technician_id, is_active),
    INDEX idx_assignments_complaint  (complaint_id)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- 5. complaint_history  (append-only audit trail)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint_history (
    history_id       INT          NOT NULL AUTO_INCREMENT,
    complaint_id     INT          NOT NULL,
    previous_status  ENUM('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS',
                          'RESOLVED', 'REJECTED', 'CANCELLED') NULL,   -- NULL for the first entry
    new_status       ENUM('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS',
                          'RESOLVED', 'REJECTED', 'CANCELLED') NOT NULL,
    updated_by       INT          NOT NULL,
    changed_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks          VARCHAR(500) NULL,
    PRIMARY KEY (history_id),
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id),
    CONSTRAINT fk_history_user      FOREIGN KEY (updated_by)   REFERENCES users (user_id),
    INDEX idx_history_complaint (complaint_id, history_id)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- Triggers: integrity rules that must hold even if someone bypasses the Java code
-- ----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_complaints_terminal_status;
DROP TRIGGER IF EXISTS trg_history_no_update;
DROP TRIGGER IF EXISTS trg_history_no_delete;

DELIMITER $$

-- RESOLVED, REJECTED and CANCELLED are final: their status can never change again.
CREATE TRIGGER trg_complaints_terminal_status
BEFORE UPDATE ON complaints
FOR EACH ROW
BEGIN
    IF OLD.status IN ('RESOLVED', 'REJECTED', 'CANCELLED') AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A RESOLVED, REJECTED or CANCELLED complaint cannot change status';
    END IF;
END$$

-- History rows can never be overwritten or removed.
CREATE TRIGGER trg_history_no_update
BEFORE UPDATE ON complaint_history
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'complaint_history is append-only';
END$$

CREATE TRIGGER trg_history_no_delete
BEFORE DELETE ON complaint_history
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'complaint_history is append-only';
END$$

DELIMITER ;
