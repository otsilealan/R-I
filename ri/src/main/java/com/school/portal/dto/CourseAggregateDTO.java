package com.school.portal.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class CourseAggregateDTO {
    private Long courseId;
    private String courseName;
    private int studentCount;
    private BigDecimal classAverage;
    private OffsetDateTime lastRefreshedAt;
    private List<StudentRankingDTO> studentRankings;
}
