-- V4: Grades — resolved letter grade per exam result

CREATE TABLE grades (
    id                   BIGSERIAL PRIMARY KEY,
    exam_result_id       BIGINT        NOT NULL,
    grade_scale_entry_id BIGINT        NOT NULL,
    letter_grade         VARCHAR(5)    NOT NULL,
    grade_points         NUMERIC(3, 2) NOT NULL,
    resolved_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_by          VARCHAR(100)  NOT NULL,
    CONSTRAINT fk_grades_exam_result       FOREIGN KEY (exam_result_id)
        REFERENCES exam_results (id),
    CONSTRAINT fk_grades_grade_scale_entry FOREIGN KEY (grade_scale_entry_id)
        REFERENCES grade_scale_entries (id),
    CONSTRAINT uq_grades_exam_result_id    UNIQUE (exam_result_id)
);

CREATE INDEX idx_grades_exam_result_id       ON grades (exam_result_id);
CREATE INDEX idx_grades_grade_scale_entry_id ON grades (grade_scale_entry_id);
