-- V3: Exam results (append-only; corrections via supersedes_id)

CREATE TABLE exam_results (
    id            BIGSERIAL PRIMARY KEY,
    student_id    BIGINT        NOT NULL,
    course_id     BIGINT        NOT NULL,
    exam_name     VARCHAR(200)  NOT NULL,
    raw_score     NUMERIC(5, 2) NOT NULL,
    max_score     NUMERIC(5, 2) NOT NULL,
    supersedes_id BIGINT,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by    VARCHAR(100)  NOT NULL,
    CONSTRAINT fk_exam_results_student    FOREIGN KEY (student_id)
        REFERENCES students (id),
    CONSTRAINT fk_exam_results_course     FOREIGN KEY (course_id)
        REFERENCES courses (id),
    CONSTRAINT fk_exam_results_supersedes FOREIGN KEY (supersedes_id)
        REFERENCES exam_results (id),
    CONSTRAINT chk_exam_results_raw_score  CHECK (raw_score >= 0),
    CONSTRAINT chk_exam_results_max_score  CHECK (raw_score <= max_score),
    CONSTRAINT chk_exam_results_no_self    CHECK (supersedes_id <> id)
);

CREATE INDEX idx_exam_results_student_id    ON exam_results (student_id);
CREATE INDEX idx_exam_results_course_id     ON exam_results (course_id);
CREATE INDEX idx_exam_results_supersedes_id ON exam_results (supersedes_id);
