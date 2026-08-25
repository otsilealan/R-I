-- R__create_triggers: Repeatable — DB trigger to auto-revert approved report cards
-- when a grade correction is inserted for the same student and period.

CREATE OR REPLACE FUNCTION fn_revert_report_card_on_correction()
RETURNS TRIGGER AS $$
BEGIN
    -- When a correction row is inserted (supersedes_id IS NOT NULL),
    -- find any APPROVED report card for the affected student/period and revert to DRAFT.
    IF NEW.supersedes_id IS NOT NULL THEN
        UPDATE report_cards rc
        SET    status      = 'DRAFT',
               approved_at = NULL,
               approved_by = NULL
        FROM   courses c
        WHERE  c.id                   = NEW.course_id
          AND  rc.student_id          = NEW.student_id
          AND  rc.reporting_period_id = c.reporting_period_id
          AND  rc.status              = 'APPROVED';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_revert_report_card_on_correction ON exam_results;
CREATE TRIGGER trg_revert_report_card_on_correction
    AFTER INSERT ON exam_results
    FOR EACH ROW
    EXECUTE FUNCTION fn_revert_report_card_on_correction();
