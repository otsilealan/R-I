package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T038 — Verifies all Flyway migrations apply cleanly to a fresh container.
 * Constitution Principle I: every migration must succeed; no rank gaps allowed.
 */
class FlywayChecksumTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMigrationsHaveSuccessTrue() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT installed_rank, description, success FROM flyway_schema_history "
                        + "ORDER BY installed_rank");
        assertThat(rows).isNotEmpty();
        rows.forEach(r -> assertThat((Boolean) r.get("success"))
                .as("Migration '%s' (rank %s) must succeed", r.get("description"), r.get("installed_rank"))
                .isTrue());
    }

    @Test
    void noGapsInInstalledRank() {
        List<Integer> ranks = jdbc.queryForList(
                "SELECT installed_rank FROM flyway_schema_history ORDER BY installed_rank",
                Integer.class);
        for (int i = 0; i < ranks.size(); i++) {
            assertThat(ranks.get(i)).as("Rank gap detected at position %d", i).isEqualTo(i + 1);
        }
    }
}
