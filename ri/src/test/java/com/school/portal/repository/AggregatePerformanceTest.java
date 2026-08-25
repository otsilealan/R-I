package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T073 — Performance guard: aggregate queries must not produce seq-scans on
 * exam_results when the table exceeds 10,000 rows.
 */
class AggregatePerformanceTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ReportingPeriodRepository periodRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private ExamResultRepository examResultRepo;
    @Autowired private AggregateRepository aggregateRepo;

    @Test
    void aggregateQuery_usesIndexScan_notSeqScan() {
        // Seed one course with enough rows to trigger planner index preference
        Teacher t = new Teacher();
        t.setEmployeeNumber("PERF-T-" + System.nanoTime());
        t.setFirstName("P"); t.setLastName("T"); t.setEmail("pt" + System.nanoTime() + "@s.edu");
        Teacher teacher = teacherRepo.save(t);

        ReportingPeriod rp = new ReportingPeriod();
        rp.setName("Perf " + System.nanoTime()); rp.setAcademicYear("2026-2027");
        rp.setStartDate(LocalDate.now().minusDays(30));
        rp.setEndDate(LocalDate.now().plusDays(30));
        rp.setGradeSubmissionClose(OffsetDateTime.now().plusDays(30));
        periodRepo.save(rp);

        Course c = new Course();
        c.setCourseCode("PRF-" + System.nanoTime()); c.setCourseName("Perf Course");
        c.setReportingPeriod(rp); c.setTeacher(teacher);
        c.setMaxScore(new BigDecimal("100.00"));
        Long courseId = courseRepo.save(c).getId();

        // Insert 200 students × 50 exams = 10,000 rows using bulk JDBC
        List<Object[]> batch = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Student s = new Student();
            s.setStudentNumber("PRF-" + i + "-" + System.nanoTime());
            s.setFirstName("P" + i); s.setLastName("S");
            s.setDateOfBirth(LocalDate.of(2008, 1, 1));
            s.setEmail("p" + i + System.nanoTime() + "@s.edu");
            Long sid = studentRepo.save(s).getId();

            Enrollment e = new Enrollment();
            e.setStudent(s); e.setCourse(courseRepo.findById(courseId).get());
            e.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepo.save(e);

            for (int j = 0; j < 50; j++) {
                batch.add(new Object[]{sid, courseId, "Exam" + j,
                        BigDecimal.valueOf(50 + (j % 50)), new BigDecimal("100.00"), "seed"});
            }
        }
        jdbc.batchUpdate(
            "INSERT INTO exam_results (student_id, course_id, exam_name, raw_score, max_score, created_by) "
          + "VALUES (?, ?, ?, ?, ?, ?)", batch);

        // Refresh the materialized view
        aggregateRepo.refreshMaterializedView();

        // Check query plan — must use index scan, not seq scan
        String plan = jdbc.queryForObject(
            "EXPLAIN SELECT * FROM mv_course_aggregates WHERE course_id = " + courseId,
            String.class);

        assertThat(plan).as("Query plan should use index, not Seq Scan on mv_course_aggregates")
                .doesNotContain("Seq Scan on mv_course_aggregates");
    }
}
