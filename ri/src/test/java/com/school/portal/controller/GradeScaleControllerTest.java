package com.school.portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.portal.AbstractIntegrationTest;
import com.school.portal.dto.CreateGradeScaleRequest;
import com.school.portal.dto.GradeScaleEntryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T037 — Integration test covering Quickstart Scenario 2.
 * Valid scale → 201. Overlapping scale → 400. List → returns created scale.
 */
class GradeScaleControllerTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void createValidGradeScale_returns201() {
        CreateGradeScaleRequest req = validScaleRequest("Standard 2026-IT");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/grade-scales", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void listGradeScales_returnsCreatedScale() {
        restTemplate.postForEntity("/api/v1/grade-scales", validScaleRequest("List Test Scale"), String.class);
        ResponseEntity<List> resp = restTemplate.getForEntity("/api/v1/grade-scales", List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test
    void createOverlappingGradeScale_returns400() {
        CreateGradeScaleRequest req = new CreateGradeScaleRequest();
        req.setName("Overlap Scale Test");
        req.setEntries(List.of(
                entry("A", "80.00", "100.00", "4.00"),
                entry("B", "75.00",  "90.00", "3.00")
        ));
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/grade-scales", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // helpers
    private CreateGradeScaleRequest validScaleRequest(String name) {
        CreateGradeScaleRequest req = new CreateGradeScaleRequest();
        req.setName(name);
        req.setEntries(List.of(
                entry("A", "90.00", "100.00", "4.00"),
                entry("B", "80.00",  "89.99", "3.00"),
                entry("C", "70.00",  "79.99", "2.00"),
                entry("D", "60.00",  "69.99", "1.00"),
                entry("F",  "0.00",  "59.99", "0.00")
        ));
        return req;
    }

    private GradeScaleEntryDTO entry(String l, String min, String max, String pts) {
        GradeScaleEntryDTO dto = new GradeScaleEntryDTO();
        dto.setLetterGrade(l);
        dto.setMinScore(new BigDecimal(min));
        dto.setMaxScore(new BigDecimal(max));
        dto.setGradePoints(new BigDecimal(pts));
        dto.setPass(!"F".equals(l));
        return dto;
    }
}
