package com.school.portal.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class GradeScaleDTO {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private OffsetDateTime createdAt;
    private String createdBy;
    private List<GradeScaleEntryDTO> entries;
}
