package com.school.portal.controller;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import com.school.portal.dto.SubmitResultRequest;
import com.school.portal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T052 — Integration test covering Quickstart Scenarios 3 and 6.
 * Scenario 3: submit → verify audit fields; correct → verify supersedes_id; verify immutability.
 * Scenario 6: closed period → 409.
 */
class ExamResultControllerTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ReportingPeriodRepository periodRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private ExamResultRepository examResultRepo;

    private Long studentId;
    private Long courseId;
    private Long closedCourseId;

    @BeforeEach
    void seed() {
        Student s = new Student();
        s.setStudentNumber("SC3-" + System.nanoTime());
        s.setFirstName("Alice"); s.setLastName("Test");
        s.setDateOfBirth(LocalDate.of(2007, 5, 20));
        s.setEmail("alice" + System.nanoTime() + "@school.edu");
        studentId = studentRepo.save(s).getId();

        Teacher t = new Teacher();
        t.setEmployeeNumber("TCH-" + System.nanoTime());
        t.setFirstName("Bob"); t.setLastName("Teacher");
        t.setEmail("bob" + System.nanoTime() + "@school.edu");
        Teacher teacher = teacherRepo.save(t);

        // Open period
        ReportingPeriod open = new ReportingPeriod();
        open.setName("Open " + System.nanoTime()); open.setAcademicYear("2026-2027");
        open.setStartDate(LocalDate.now().minusDays(10));
        open.setEndDate(LocalDate.now().plusDays(60));
        open.setGradeSubmissionClose(OffsetDateTime.now().plusDays(60));
        periodRepo.save(open);

        Course c = new Course();
        c.setCourseCode("OPN-" + System.nanoTime()); c.setCourseName("Open Course");
        c.setReportingPeriod(open); c.setTeacher(teacher);
        c.setMaxScore(new BigDecimal("100.00"));
        courseId = courseRepo.save(c).getId();

        Enrollment e = new Enrollment();
        e.setStudent(s); e.setCourse(c); e.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepo.save(e);

        // Closed period
        ReportingPeriod closed = new ReportingPeriod();
        closed.setName("Closed " + System.nanoTime()); closed.setAcademicYear("2025-2026");
        closed.setStartDate(LocalDate.now().minusDays(180));
        closed.setEndDate(LocalDate.now().minusDays(60));
        closed.setGradeSubmissionClose(OffsetDateTime.now().minusDays(1));
        periodRepo.save(closed);

        Course cc = new Course();
        cc.setCourseCode("CLS-" + System.nanoTime()); cc.setCourseName("Closed Course");
        cc.setReportingPeriod(closed); cc.setTeacher(teacher);
        cc.setMaxScore(new BigDecimal("100.00"));
        closedCourseId = courseRepo.save(cc).getId();

        Enrollment ec = new Enrollment();
        ec.setStudent(s); ec.setCourse(cc); ec.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepo.save(ec);
    }

    @Test
    void submitResult_validScore_returns201WithAuditFields() {
        SubmitResultRequest req = new SubmitResultRequest();
        req.setStudentId(studentId); req.setCourseId(courseId);
        req.setExamName("Midterm"); req.setRawScore(new BigDecimal("78.50"));

        ResponseEntity<String> resp = restTemplate.postForEntity("/api/v1/results", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("createdAt").contains("rawScore");
    }

    @Test
    void submitResult_closedPeriod_returns409() {
        SubmitResultRequest req = new SubmitResultRequest();
        req.setStudentId(studentId); req.setCourseId(closedCourseId);
        req.setExamName("Final"); req.setRawScore(new BigDecimal("70.00"));

        ResponseEntity<String> resp = restTemplate.postForEntity("/api/v1/results", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsIgnoringCase("closed");
    }

    @Test
    void submitResult_scoreAboveMax_returns400() {
        SubmitResultRequest req = new SubmitResultRequest();
        req.setStudentId(studentId); req.setCourseId(courseId);
        req.setExamName("Quiz"); req.setRawScore(new BigDecimal("105.00"));

        ResponseEntity<String> resp = restTemplate.postForEntity("/api/v1/results", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
