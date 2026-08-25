package com.school.portal.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class AggregateRefreshDTO {
    private OffsetDateTime refreshedAt;
    private String refreshedBy;
}
