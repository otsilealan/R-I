package com.school.portal.dto;

import com.school.portal.domain.entity.GradeScale;
import com.school.portal.domain.entity.GradeScaleEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GradeScaleMapper {

    @Mapping(target = "active", source = "active")
    GradeScaleDTO toDTO(GradeScale entity);

    @Mapping(target = "pass", source = "passIndicator")
    GradeScaleEntryDTO entryToDTO(GradeScaleEntry entry);
}
