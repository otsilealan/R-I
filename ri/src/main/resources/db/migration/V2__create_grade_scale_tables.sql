-- V2: Grade scale tables

CREATE TABLE grade_scales (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL,
    CONSTRAINT uq_grade_scales_name UNIQUE (name)
);

CREATE TABLE grade_scale_entries (
    id              BIGSERIAL PRIMARY KEY,
    grade_scale_id  BIGINT        NOT NULL,
    letter_grade    VARCHAR(5)    NOT NULL,
    min_score       NUMERIC(5, 2) NOT NULL,
    max_score       NUMERIC(5, 2) NOT NULL,
    grade_points    NUMERIC(3, 2) NOT NULL,
    pass_indicator  BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order   INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT fk_grade_scale_entries_scale FOREIGN KEY (grade_scale_id)
        REFERENCES grade_scales (id),
    CONSTRAINT uq_grade_scale_entries_scale_letter UNIQUE (grade_scale_id, letter_grade),
    CONSTRAINT chk_grade_scale_entries_range CHECK (max_score > min_score)
);

CREATE INDEX idx_grade_scale_entries_grade_scale_id ON grade_scale_entries (grade_scale_id);
