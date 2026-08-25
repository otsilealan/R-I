package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.ReportCardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * T067 — Unique constraint test: two inserts for same student/period
 * must fail with DataIntegrityViolationException.
 */
class ReportCardRepositoryTest extends AbstractIntegrationTest {

    @Autowired private ReportCardRepository rcRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ReportingPeriodRepository periodRepo;
    @Autowired private CourseRepository courseRepo;

    private Student student;
    private ReportingPeriod period;

    @BeforeEach
    void seed() {
        Student s = new Student();
        s.setStudentNumber("RC-" + System.nanoTime());
        s.setFirstName("RC"); s.setLastName("Student");
        s.setDateOfBirth(LocalDate.of(2008, 1, 1));
        s.setEmail("rcs" + System.nanoTime() + "@school.edu");
        student = studentRepo.save(s);

        ReportingPeriod rp = new ReportingPeriod();
        rp.setName("RC Period " + System.nanoTime()); rp.setAcademicYear("2026-2027");
        rp.setStartDate(LocalDate.now().minusDays(10));
        rp.setEndDate(LocalDate.now().plusDays(60));
        rp.setGradeSubmissionClose(OffsetDateTime.now().plusDays(60));
        period = periodRepo.save(rp);
    }

    @Test
    void uniqueConstraint_preventsDuplicateReportCard() {
        // First insert succeeds
        ReportCard first = new ReportCard();
        first.setStudent(student); first.setReportingPeriod(period);
        first.setStatus(ReportCardStatus.DRAFT); first.setGeneratedBy("admin");
        rcRepo.saveAndFlush(first);

        // Second insert for same student/period must fail
        ReportCard second = new ReportCard();
        second.setStudent(student); second.setReportingPeriod(period);
        second.setStatus(ReportCardStatus.DRAFT); second.setGeneratedBy("admin");

        assertThatThrownBy(() -> rcRepo.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByStudentIdAndReportingPeriodId_returnsCorrectRecord() {
        ReportCard rc = new ReportCard();
        rc.setStudent(student); rc.setReportingPeriod(period);
        rc.setStatus(ReportCardStatus.DRAFT); rc.setGeneratedBy("admin");
        ReportCard saved = rcRepo.save(rc);

        var found = rcRepo.findByStudentIdAndReportingPeriodId(student.getId(), period.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }
}
