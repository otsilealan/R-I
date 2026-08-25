package com.school.portal.repository;

import com.school.portal.dto.StudentRankingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AggregateRepository {

    private final JdbcTemplate jdbc;

    @Autowired
    public AggregateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns per-student aggregates for a course from the materialized view. */
    @Transactional(readOnly = true)
    public List<StudentRankingDTO> findCourseAggregates(Long courseId) {
        return jdbc.query(
            "SELECT student_id, course_id, exam_count, percentage_avg, class_rank "
          + "FROM mv_course_aggregates WHERE course_id = ? ORDER BY class_rank",
            (rs, row) -> {
                StudentRankingDTO dto = new StudentRankingDTO();
                dto.setStudentId(rs.getLong("student_id"));
                dto.setAverage(rs.getBigDecimal("percentage_avg"));
                dto.setRank(rs.getInt("class_rank"));
                return dto;
            }, courseId);
    }

    /** Returns the class average for a course. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findClassAverage(Long courseId) {
        List<BigDecimal> results = jdbc.queryForList(
            "SELECT AVG(percentage_avg) FROM mv_course_aggregates WHERE course_id = ?",
            BigDecimal.class, courseId);
        return results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.get(0));
    }

    /** Returns the student count for a course. */
    @Transactional(readOnly = true)
    public int countStudentsInCourse(Long courseId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM mv_course_aggregates WHERE course_id = ?",
            Integer.class, courseId);
        return count != null ? count : 0;
    }

    /** Returns the last refresh timestamp for mv_course_aggregates, if recorded. */
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> findLastRefreshedAt() {
        List<OffsetDateTime> results = jdbc.queryForList(
            "SELECT last_refreshed_at FROM mv_refresh_log "
          + "WHERE view_name = 'mv_course_aggregates'",
            OffsetDateTime.class);
        return results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.get(0));
    }

    /**
     * Refreshes the materialized view CONCURRENTLY (non-blocking).
     * Requires the unique index on mv_course_aggregates to exist.
     */
    @Transactional
    public void refreshMaterializedView() {
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_course_aggregates");
    }

    /** Records the refresh timestamp in mv_refresh_log. */
    @Transactional
    public void recordRefresh(String refreshedBy) {
        jdbc.update(
            "INSERT INTO mv_refresh_log (view_name, last_refreshed_at, refreshed_by) "
          + "VALUES ('mv_course_aggregates', now(), ?) "
          + "ON CONFLICT (view_name) DO UPDATE "
          + "SET last_refreshed_at = EXCLUDED.last_refreshed_at, "
          + "    refreshed_by = EXCLUDED.refreshed_by",
            refreshedBy);
    }
}
