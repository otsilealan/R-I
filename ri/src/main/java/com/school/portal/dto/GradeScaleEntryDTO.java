package com.school.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class GradeScaleEntryDTO {
    private Long id;
    @NotBlank private String letterGrade;
    @NotNull @DecimalMin("0.00") private BigDecimal minScore;
    @NotNull private BigDecimal maxScore;
    @NotNull private BigDecimal gradePoints;
    private boolean pass;
    private int displayOrder;
}
