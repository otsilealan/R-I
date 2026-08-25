package com.school.portal.controller;

import com.school.portal.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T059 — Integration test covering Quickstart Scenario 4 (aggregates endpoint).
 */
class AggregateControllerTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void refreshAggregates_returns200WithTimestamp() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/results/aggregates/refresh", null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("refreshedAt").contains("refreshedBy");
    }

    @Test
    void getCourseAggregates_unknownCourse_returns404() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/v1/results/aggregates?courseId=99999", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
