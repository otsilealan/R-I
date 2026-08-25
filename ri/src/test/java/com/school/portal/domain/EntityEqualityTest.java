package com.school.portal.domain;

import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T076 — Verifies all entities use business-key equals/hashCode, not surrogate id.
 * Two transient instances with the same business key must be equal.
 */
class EntityEqualityTest {

    @Test
    void student_equalityByStudentNumber() {
        Student a = new Student(); a.setStudentNumber("STU-001");
        Student b = new Student(); b.setStudentNumber("STU-001");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void student_differentStudentNumber_notEqual() {
        Student a = new Student(); a.setStudentNumber("STU-001");
        Student b = new Student(); b.setStudentNumber("STU-002");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void teacher_equalityByEmployeeNumber() {
        Teacher a = new Teacher(); a.setEmployeeNumber("EMP-001");
        Teacher b = new Teacher(); b.setEmployeeNumber("EMP-001");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void gradeScale_equalityByName() {
        GradeScale a = new GradeScale(); a.setName("Standard 2026");
        GradeScale b = new GradeScale(); b.setName("Standard 2026");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void gradeScaleEntry_equalityByScaleAndLetter() {
        GradeScale scale = new GradeScale(); scale.setName("S");
        GradeScaleEntry a = new GradeScaleEntry();
        a.setGradeScale(scale); a.setLetterGrade("A");
        GradeScaleEntry b = new GradeScaleEntry();
        b.setGradeScale(scale); b.setLetterGrade("A");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void reportingPeriod_equalityByNameAndYear() {
        ReportingPeriod a = new ReportingPeriod();
        a.setName("Sem 1"); a.setAcademicYear("2026-2027");
        ReportingPeriod b = new ReportingPeriod();
        b.setName("Sem 1"); b.setAcademicYear("2026-2027");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void enrollment_equalityByStudentAndCourse() {
        Student s = new Student(); s.setStudentNumber("STU-001");
        Course c = new Course(); c.setCourseCode("MATH101");
        ReportingPeriod rp = new ReportingPeriod();
        rp.setName("P1"); rp.setAcademicYear("2026-2027");
        c.setReportingPeriod(rp);

        Enrollment a = new Enrollment(); a.setStudent(s); a.setCourse(c);
        Enrollment b = new Enrollment(); b.setStudent(s); b.setCourse(c);
        assertThat(a).isEqualTo(b);
    }
}
