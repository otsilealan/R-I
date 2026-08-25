-- V5: Report cards, report card items, and materialized view refresh log

CREATE TABLE report_cards (
    id                    BIGSERIAL PRIMARY KEY,
    student_id            BIGINT        NOT NULL,
    reporting_period_id   BIGINT        NOT NULL,
    status                VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    overall_average       NUMERIC(5, 2),
    overall_grade_points  NUMERIC(3, 2),
    has_incomplete_courses BOOLEAN      NOT NULL DEFAULT FALSE,
    generated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    generated_by          VARCHAR(100)  NOT NULL,
    approved_at           TIMESTAMPTZ,
    approved_by           VARCHAR(100),
    CONSTRAINT fk_report_cards_student         FOREIGN KEY (student_id)
        REFERENCES students (id),
    CONSTRAINT fk_report_cards_reporting_period FOREIGN KEY (reporting_period_id)
        REFERENCES reporting_periods (id),
    CONSTRAINT uq_report_cards_student_period   UNIQUE (student_id, reporting_period_id),
    CONSTRAINT chk_report_cards_status          CHECK (status IN ('DRAFT', 'APPROVED'))
);

CREATE INDEX idx_report_cards_student_id          ON report_cards (student_id);
CREATE INDEX idx_report_cards_reporting_period_id ON report_cards (reporting_period_id);

CREATE TABLE report_card_items (
    id              BIGSERIAL PRIMARY KEY,
    report_card_id  BIGINT        NOT NULL,
    course_id       BIGINT        NOT NULL,
    course_average  NUMERIC(5, 2),
    letter_grade    VARCHAR(5),
    grade_points    NUMERIC(3, 2),
    is_incomplete   BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_report_card_items_report_card FOREIGN KEY (report_card_id)
        REFERENCES report_cards (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_card_items_course      FOREIGN KEY (course_id)
        REFERENCES courses (id),
    CONSTRAINT uq_report_card_items_card_course  UNIQUE (report_card_id, course_id)
);

CREATE INDEX idx_report_card_items_report_card_id ON report_card_items (report_card_id);
CREATE INDEX idx_report_card_items_course_id      ON report_card_items (course_id);

CREATE TABLE mv_refresh_log (
    view_name        VARCHAR(100) PRIMARY KEY,
    last_refreshed_at TIMESTAMPTZ NOT NULL,
    refreshed_by     VARCHAR(100) NOT NULL
);
