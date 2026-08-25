package com.school.portal.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReportCardDTO {
    private Long id;
    private String status;
    private OffsetDateTime generatedAt;
    private String generatedBy;
    private OffsetDateTime approvedAt;
    private String approvedBy;
    private boolean hasIncompleteCourses;
    private StudentSummaryDTO student;
    private ReportingPeriodSummaryDTO reportingPeriod;
    private BigDecimal overallAverage;
    private BigDecimal overallGradePoints;
    private String overallLetterGrade;
    private List<CourseResultDTO> courseResults;

    @Data
    public static class StudentSummaryDTO {
        private Long id;
        private String studentNumber;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
    }

    @Data
    public static class ReportingPeriodSummaryDTO {
        private Long id;
        private String name;
        private String academicYear;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
