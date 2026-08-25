package com.school.portal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreateGradeScaleRequest {
    @NotBlank
    private String name;
    private String description;
    @NotEmpty
    @Valid
    private List<GradeScaleEntryDTO> entries;
}
