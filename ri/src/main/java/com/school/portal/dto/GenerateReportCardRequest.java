package com.school.portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateReportCardRequest {
    @NotNull private Long studentId;
    @NotNull private Long reportingPeriodId;
}
