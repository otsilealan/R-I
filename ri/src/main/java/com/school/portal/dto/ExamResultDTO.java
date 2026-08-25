package com.school.portal.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class ExamResultDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseName;
    private String examName;
    private BigDecimal rawScore;
    private BigDecimal maxScore;
    private Long supersedesId;
    private OffsetDateTime createdAt;
    private String createdBy;
    private String letterGrade;
    private BigDecimal gradePoints;
}
