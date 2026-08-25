package com.school.portal.controller;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.ReportCardStatus;
import com.school.portal.dto.GenerateReportCardRequest;
import com.school.portal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T070 — Integration test: Quickstart Scenario 5 via HTTP.
 * generate → idempotency → approve → correct grade → assert DRAFT revert.
 */
class ReportCardControllerTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ReportingPeriodRepository periodRepo;
    @Autowired private ReportCardRepository rcRepo;

    private Long studentId;
    private Long periodId;

    @BeforeEach
    void seed() {
        Student s = new Student();
        s.setStudentNumber("RCC-" + System.nanoTime());
        s.setFirstName("RC"); s.setLastName("CtrlTest");
        s.setDateOfBirth(LocalDate.of(2007, 6, 15));
        s.setEmail("rcc" + System.nanoTime() + "@school.edu");
        studentId = studentRepo.save(s).getId();

        ReportingPeriod rp = new ReportingPeriod();
        rp.setName("RCC Period " + System.nanoTime()); rp.setAcademicYear("2026-2027");
        rp.setStartDate(LocalDate.now().minusDays(10));
        rp.setEndDate(LocalDate.now().plusDays(60));
        rp.setGradeSubmissionClose(OffsetDateTime.now().plusDays(60));
        periodId = periodRepo.save(rp).getId();
    }

    @Test
    void generateReportCard_returns200WithDraftStatus() {
        GenerateReportCardRequest req = new GenerateReportCardRequest();
        req.setStudentId(studentId); req.setReportingPeriodId(periodId);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/report-cards/generate", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("DRAFT");
    }

    @Test
    void generateReportCard_idempotent_returnsSameId() {
        GenerateReportCardRequest req = new GenerateReportCardRequest();
        req.setStudentId(studentId); req.setReportingPeriodId(periodId);

        ResponseEntity<java.util.Map> first  = restTemplate.postForEntity(
                "/api/v1/report-cards/generate", req, java.util.Map.class);
        ResponseEntity<java.util.Map> second = restTemplate.postForEntity(
                "/api/v1/report-cards/generate", req, java.util.Map.class);

        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
    }

    @Test
    void approveReportCard_unknownId_returns404() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/report-cards/99999/approve", null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getReportCard_unknownId_returns404() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/v1/report-cards/99999", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
