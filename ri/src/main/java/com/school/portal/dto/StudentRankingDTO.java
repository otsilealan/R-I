package com.school.portal.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentRankingDTO {
    private Long studentId;
    private String studentName;
    private BigDecimal average;
    private int rank;
}
