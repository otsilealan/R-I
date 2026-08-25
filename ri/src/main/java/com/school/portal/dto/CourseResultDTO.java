package com.school.portal.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CourseResultDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String teacherName;
    private BigDecimal courseAverage;
    private String letterGrade;
    private BigDecimal gradePoints;
    private Integer classRank;
    private int classSize;
    private boolean isIncomplete;
}
