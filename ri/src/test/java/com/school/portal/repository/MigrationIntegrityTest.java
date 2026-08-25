package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T025 — Verifies all Flyway migrations apply cleanly and FK indexes exist.
 * Constitution Principle I: no failed migrations, no schema drift.
 */
class MigrationIntegrityTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMigrationsSucceed() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT installed_rank, version, description, success "
                        + "FROM flyway_schema_history ORDER BY installed_rank");

        assertThat(history).isNotEmpty();
        history.forEach(row ->
                assertThat(row.get("success"))
                        .as("Migration %s should succeed", row.get("description"))
                        .isEqualTo(true));
    }

    @Test
    void noGapsInInstalledRank() {
        List<Integer> ranks = jdbc.queryForList(
                "SELECT installed_rank FROM flyway_schema_history ORDER BY installed_rank",
                Integer.class);
        for (int i = 0; i < ranks.size(); i++) {
            assertThat(ranks.get(i)).isEqualTo(i + 1);
        }
    }

    @Test
    void fkIndexesExistOnExamResults() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'exam_results' "
                        + "AND indexname LIKE 'idx_%'",
                String.class);
        assertThat(indexes).contains(
                "idx_exam_results_student_id",
                "idx_exam_results_course_id",
                "idx_exam_results_supersedes_id");
    }

    @Test
    void activeExamResultsViewExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.views "
                        + "WHERE table_name = 'v_active_exam_results'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void reportCardRevertTriggerExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.triggers "
                        + "WHERE trigger_name = 'trg_revert_report_card_on_correction'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
