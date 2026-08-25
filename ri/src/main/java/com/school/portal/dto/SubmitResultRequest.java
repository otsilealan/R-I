package com.school.portal.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SubmitResultRequest {
    @NotNull private Long studentId;
    @NotNull private Long courseId;
    @NotBlank @Size(max = 200) private String examName;
    @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) private BigDecimal rawScore;
}
