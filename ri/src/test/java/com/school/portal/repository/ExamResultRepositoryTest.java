package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T050 — Integration test: persist original, persist correction,
 * verify v_active_exam_results shows only the correction.
 */
class ExamResultRepositoryTest extends AbstractIntegrationTest {

    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ReportingPeriodRepository periodRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ExamResultRepository examResultRepository;
    @Autowired private JdbcTemplate jdbc;

    private Student student;
    private Course course;

    @BeforeEach
    void seed() {
        Student s = new Student();
        s.setStudentNumber("TEST-" + System.nanoTime());
        s.setFirstName("Test"); s.setLastName("Student");
        s.setDateOfBirth(LocalDate.of(2008, 1, 1));
        s.setEmail("test" + System.nanoTime() + "@school.edu");
        student = studentRepository.save(s);

        Teacher t = new Teacher();
        t.setEmployeeNumber("EMP-" + System.nanoTime());
        t.setFirstName("Test"); t.setLastName("Teacher");
        t.setEmail("teacher" + System.nanoTime() + "@school.edu");
        Teacher teacher = teacherRepository.save(t);

        ReportingPeriod rp = new ReportingPeriod();
        rp.setName("Test Period " + System.nanoTime());
        rp.setAcademicYear("2026-2027");
        rp.setStartDate(LocalDate.now().minusDays(30));
        rp.setEndDate(LocalDate.now().plusDays(30));
        rp.setGradeSubmissionClose(OffsetDateTime.now().plusDays(30));
        ReportingPeriod period = periodRepository.save(rp);

        Course c = new Course();
        c.setCourseCode("TST-" + System.nanoTime());
        c.setCourseName("Test Course");
        c.setReportingPeriod(period);
        c.setTeacher(teacher);
        c.setMaxScore(new BigDecimal("100.00"));
        course = courseRepository.save(c);

        Enrollment e = new Enrollment();
        e.setStudent(student); e.setCourse(course);
        e.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(e);
    }

    @Test
    void correction_isActiveAndOriginalIsSuperseded() {
        // Persist original
        ExamResult original = new ExamResult();
        original.setStudent(student); original.setCourse(course);
        original.setExamName("Midterm");
        original.setRawScore(new BigDecimal("78.50"));
        original.setMaxScore(new BigDecimal("100.00"));
        original.setCreatedBy("teacher");
        ExamResult savedOriginal = examResultRepository.save(original);

        // Persist correction
        ExamResult correction = new ExamResult();
        correction.setStudent(student); correction.setCourse(course);
        correction.setExamName("Midterm");
        correction.setRawScore(new BigDecimal("82.00"));
        correction.setMaxScore(new BigDecimal("100.00"));
        correction.setSupersedes(savedOriginal);
        correction.setCreatedBy("teacher");
        ExamResult savedCorrection = examResultRepository.save(correction);

        // Both rows must be in exam_results (immutability)
        assertThat(examResultRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);

        // Active query must return only the correction
        List<ExamResult> active = examResultRepository
                .findActiveByStudentAndCourse(student.getId(), course.getId());
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getId()).isEqualTo(savedCorrection.getId());
        assertThat(active.get(0).getRawScore()).isEqualByComparingTo("82.00");

        // Original must still exist (not deleted)
        assertThat(examResultRepository.findById(savedOriginal.getId())).isPresent();
    }

    @Test
    void viewActiveExamResultsShowsOnlyCorrection() {
        ExamResult original = new ExamResult();
        original.setStudent(student); original.setCourse(course);
        original.setExamName("Final");
        original.setRawScore(new BigDecimal("65.00"));
        original.setMaxScore(new BigDecimal("100.00"));
        original.setCreatedBy("teacher");
        ExamResult savedOriginal = examResultRepository.save(original);

        ExamResult correction = new ExamResult();
        correction.setStudent(student); correction.setCourse(course);
        correction.setExamName("Final");
        correction.setRawScore(new BigDecimal("70.00"));
        correction.setMaxScore(new BigDecimal("100.00"));
        correction.setSupersedes(savedOriginal);
        correction.setCreatedBy("teacher");
        examResultRepository.save(correction);

        // Query the view directly
        List<Long> activeIds = jdbc.queryForList(
                "SELECT id FROM v_active_exam_results WHERE student_id = ? AND course_id = ? AND exam_name = 'Final'",
                Long.class, student.getId(), course.getId());

        assertThat(activeIds).doesNotContain(savedOriginal.getId());
    }
}
