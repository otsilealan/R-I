-- V1: Core tables — students, teachers, reporting_periods, courses, enrollments

CREATE TABLE students (
    id             BIGSERIAL PRIMARY KEY,
    student_number VARCHAR(20)  NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    date_of_birth  DATE         NOT NULL,
    email          VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_students_student_number UNIQUE (student_number),
    CONSTRAINT uq_students_email          UNIQUE (email)
);

CREATE TABLE teachers (
    id              BIGSERIAL PRIMARY KEY,
    employee_number VARCHAR(20)  NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_teachers_employee_number UNIQUE (employee_number),
    CONSTRAINT uq_teachers_email           UNIQUE (email)
);

CREATE TABLE reporting_periods (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL,
    academic_year           VARCHAR(9)   NOT NULL,
    start_date              DATE         NOT NULL,
    end_date                DATE         NOT NULL,
    grade_submission_close  TIMESTAMPTZ  NOT NULL,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reporting_periods_name_year UNIQUE (name, academic_year),
    CONSTRAINT chk_reporting_periods_dates    CHECK (end_date > start_date),
    CONSTRAINT chk_reporting_periods_close    CHECK (grade_submission_close >= end_date::TIMESTAMPTZ)
);

CREATE TABLE courses (
    id                  BIGSERIAL PRIMARY KEY,
    course_code         VARCHAR(20)    NOT NULL,
    course_name         VARCHAR(200)   NOT NULL,
    reporting_period_id BIGINT         NOT NULL,
    teacher_id          BIGINT         NOT NULL,
    max_score           NUMERIC(5, 2)  NOT NULL DEFAULT 100.00,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_courses_reporting_period FOREIGN KEY (reporting_period_id)
        REFERENCES reporting_periods (id),
    CONSTRAINT fk_courses_teacher          FOREIGN KEY (teacher_id)
        REFERENCES teachers (id),
    CONSTRAINT uq_courses_code_period       UNIQUE (course_code, reporting_period_id),
    CONSTRAINT chk_courses_max_score        CHECK (max_score > 0)
);

CREATE INDEX idx_courses_reporting_period_id ON courses (reporting_period_id);
CREATE INDEX idx_courses_teacher_id          ON courses (teacher_id);

CREATE TABLE enrollments (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT      NOT NULL,
    course_id   BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students (id),
    CONSTRAINT fk_enrollments_course  FOREIGN KEY (course_id)
        REFERENCES courses (id),
    CONSTRAINT uq_enrollments_student_course UNIQUE (student_id, course_id)
);

CREATE INDEX idx_enrollments_student_id ON enrollments (student_id);
CREATE INDEX idx_enrollments_course_id  ON enrollments (course_id);
