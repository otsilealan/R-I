package com.school.portal.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CorrectResultRequest {
    @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) private BigDecimal rawScore;
    @Size(max = 500) private String correctionReason;
}
