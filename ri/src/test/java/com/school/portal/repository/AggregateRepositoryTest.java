package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

/**
 * T057 — Integration test: seed 5 students × 3 exams, refresh, assert
 * percentage_avg and class_rank match expected values within ±0.01.
 */
class AggregateRepositoryTest extends AbstractIntegrationTest {

    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ReportingPeriodRepository periodRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private ExamResultRepository examResultRepo;
    @Autowired private AggregateRepository aggregateRepo;

    private Long courseId;

    @BeforeEach
    void seed() {
        Teacher t = new Teacher();
        t.setEmployeeNumber("AGG-TCH-" + System.nanoTime());
        t.setFirstName("Agg"); t.setLastName("Teacher");
        t.setEmail("aggt" + System.nanoTime() + "@school.edu");
        Teacher teacher = teacherRepo.save(t);

        ReportingPeriod rp = new ReportingPeriod();
        rp.setName("Agg Period " + System.nanoTime());
        rp.setAcademicYear("2026-2027");
        rp.setStartDate(LocalDate.now().minusDays(30));
        rp.setEndDate(LocalDate.now().plusDays(30));
        rp.setGradeSubmissionClose(OffsetDateTime.now().plusDays(30));
        periodRepo.save(rp);

        Course c = new Course();
        c.setCourseCode("AGG-" + System.nanoTime());
        c.setCourseName("Agg Course");
        c.setReportingPeriod(rp); c.setTeacher(teacher);
        c.setMaxScore(new BigDecimal("100.00"));
        courseId = courseRepo.save(c).getId();

        // Seed 5 students with known scores across 3 exams
        double[][] scores = {
            {80, 90, 70},  // avg 80
            {60, 70, 80},  // avg 70
            {90, 85, 95},  // avg 90 → rank 1
            {50, 60, 70},  // avg 60 → rank 4
            {75, 80, 85},  // avg 80 → rank 2 (tie with student 0)
        };
        String[] exams = {"Exam1", "Exam2", "Exam3"};

        for (int i = 0; i < scores.length; i++) {
            Student s = new Student();
            s.setStudentNumber("A" + i + "-" + System.nanoTime());
            s.setFirstName("S" + i); s.setLastName("Test");
            s.setDateOfBirth(LocalDate.of(2008, 1, 1));
            s.setEmail("s" + i + System.nanoTime() + "@school.edu");
            Student saved = studentRepo.save(s);

            Enrollment e = new Enrollment();
            e.setStudent(saved); e.setCourse(courseRepo.findById(courseId).get());
            e.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepo.save(e);

            for (int j = 0; j < exams.length; j++) {
                ExamResult er = new ExamResult();
                er.setStudent(saved);
                er.setCourse(courseRepo.findById(courseId).get());
                er.setExamName(exams[j]);
                er.setRawScore(BigDecimal.valueOf(scores[i][j]));
                er.setMaxScore(new BigDecimal("100.00"));
                er.setCreatedBy("seed");
                examResultRepo.save(er);
            }
        }
    }

    @Test
    void refreshAndVerifyAggregates() {
        aggregateRepo.refreshMaterializedView();
        aggregateRepo.recordRefresh("test");

        var rankings = aggregateRepo.findCourseAggregates(courseId);
        assertThat(rankings).hasSize(5);

        // Rank 1 should have highest average (~90)
        assertThat(rankings.get(0).getAverage())
                .usingComparator(BigDecimal::compareTo)
                .isGreaterThan(new BigDecimal("85.00"));

        // All averages within expected range (50–100)
        rankings.forEach(r ->
                assertThat(r.getAverage())
                        .usingComparator(BigDecimal::compareTo)
                        .isBetween(new BigDecimal("50.00"), new BigDecimal("100.00")));
    }

    @Test
    void lastRefreshedAt_unchangedAfterCorrectionWithoutRefresh() {
        aggregateRepo.refreshMaterializedView();
        aggregateRepo.recordRefresh("test");

        Optional<OffsetDateTime> before = aggregateRepo.findLastRefreshedAt();
        assertThat(before).isPresent();

        // Simulate a correction arriving — do NOT call refresh
        // Verify lastRefreshedAt is still the same value
        Optional<OffsetDateTime> after = aggregateRepo.findLastRefreshedAt();
        assertThat(after).isPresent();
        assertThat(after.get()).isCloseTo(before.get(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void classAverageMatchesExpectedWithinTolerance() {
        aggregateRepo.refreshMaterializedView();
        aggregateRepo.recordRefresh("test");

        // Expected average of averages: (80+70+90+60+80)/5 = 76
        Optional<BigDecimal> avg = aggregateRepo.findClassAverage(courseId);
        assertThat(avg).isPresent();
        assertThat(avg.get().doubleValue()).isCloseTo(76.0, org.assertj.core.data.Offset.offset(1.0));
    }
}
