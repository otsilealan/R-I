-- R__create_views: Repeatable — views and materialized view for results domain
-- Re-runs whenever this file's checksum changes.

-- Active exam results: rows not superseded by any newer correction
CREATE OR REPLACE VIEW v_active_exam_results AS
SELECT er.*
FROM exam_results er
WHERE NOT EXISTS (
    SELECT 1
    FROM exam_results newer
    WHERE newer.supersedes_id = er.id
);

-- Materialized view: per-student per-course aggregates
-- Populated via explicit REFRESH command; use mv_refresh_log to track freshness.
DROP MATERIALIZED VIEW IF EXISTS mv_course_aggregates;
CREATE MATERIALIZED VIEW mv_course_aggregates AS
SELECT
    aer.student_id,
    aer.course_id,
    COUNT(*)                                                   AS exam_count,
    AVG(aer.raw_score / NULLIF(aer.max_score, 0) * 100)       AS percentage_avg,
    RANK() OVER (
        PARTITION BY aer.course_id
        ORDER BY AVG(aer.raw_score / NULLIF(aer.max_score, 0) * 100) DESC
    )                                                          AS class_rank
FROM v_active_exam_results aer
GROUP BY aer.student_id, aer.course_id
WITH NO DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_course_aggregates_pk
    ON mv_course_aggregates (student_id, course_id);

-- Per-student per-period summary consumed by report card generation
CREATE OR REPLACE VIEW v_student_period_summary AS
SELECT
    e.student_id,
    c.reporting_period_id,
    c.id                                    AS course_id,
    ca.percentage_avg                       AS course_average,
    g.letter_grade,
    g.grade_points,
    (ca.percentage_avg IS NULL)             AS is_incomplete
FROM enrollments e
JOIN courses c ON c.id = e.course_id
LEFT JOIN mv_course_aggregates ca
    ON ca.student_id = e.student_id
   AND ca.course_id  = c.id
LEFT JOIN grades g ON g.exam_result_id = (
    SELECT aer.id
    FROM v_active_exam_results aer
    WHERE aer.student_id = e.student_id
      AND aer.course_id  = c.id
    ORDER BY aer.created_at DESC
    LIMIT 1
);
