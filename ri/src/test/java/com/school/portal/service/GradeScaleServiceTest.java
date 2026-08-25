package com.school.portal.service;

import com.school.portal.dto.CreateGradeScaleRequest;
import com.school.portal.dto.GradeScaleEntryDTO;
import com.school.portal.dto.GradeScaleMapper;
import com.school.portal.exception.BusinessRuleViolationException;
import com.school.portal.repository.GradeScaleEntryRepository;
import com.school.portal.repository.GradeScaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeScaleServiceTest {

    @Mock private GradeScaleRepository gradeScaleRepository;
    @Mock private GradeScaleEntryRepository gradeScaleEntryRepository;
    @Mock private GradeScaleMapper mapper;

    private GradeScaleService service;

    @BeforeEach
    void setUp() {
        service = new GradeScaleService(gradeScaleRepository, gradeScaleEntryRepository, mapper);
    }

    @Test
    void createGradeScale_overlappingEntries_throwsException() {
        CreateGradeScaleRequest req = new CreateGradeScaleRequest();
        req.setName("Bad Scale");
        req.setEntries(List.of(
                entry("A", new BigDecimal("80.00"), new BigDecimal("100.00"), new BigDecimal("4.00")),
                entry("B", new BigDecimal("75.00"), new BigDecimal("90.00"),  new BigDecimal("3.00"))
        ));

        assertThatThrownBy(() -> service.createGradeScale(req, "admin"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void createGradeScale_entryMaxNotGreaterThanMin_throwsException() {
        CreateGradeScaleRequest req = new CreateGradeScaleRequest();
        req.setName("Invalid Scale");
        req.setEntries(List.of(
                entry("A", new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("4.00"))
        ));

        assertThatThrownBy(() -> service.createGradeScale(req, "admin"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("maxScore must be greater than minScore");
    }

    @Test
    void deleteGradeScale_referencedByGrades_throwsException() {
        long scaleId = 1L;
        com.school.portal.domain.entity.GradeScale scale = new com.school.portal.domain.entity.GradeScale();
        scale.setName("In Use Scale");

        when(gradeScaleRepository.findById(scaleId)).thenReturn(java.util.Optional.of(scale));
        when(gradeScaleEntryRepository.isReferencedByGrades(scaleId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteGradeScale(scaleId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("referenced by existing grade records");
    }

    @Test
    void deleteGradeScale_notReferenced_succeeds() {
        long scaleId = 2L;
        com.school.portal.domain.entity.GradeScale scale = new com.school.portal.domain.entity.GradeScale();
        scale.setName("Free Scale");

        when(gradeScaleRepository.findById(scaleId)).thenReturn(java.util.Optional.of(scale));
        when(gradeScaleEntryRepository.isReferencedByGrades(scaleId)).thenReturn(false);

        assertThatNoException().isThrownBy(() -> service.deleteGradeScale(scaleId));
        verify(gradeScaleRepository).delete(scale);
    }

    // helpers
    private GradeScaleEntryDTO entry(String letter, BigDecimal min, BigDecimal max, BigDecimal pts) {
        GradeScaleEntryDTO dto = new GradeScaleEntryDTO();
        dto.setLetterGrade(letter);
        dto.setMinScore(min);
        dto.setMaxScore(max);
        dto.setGradePoints(pts);
        dto.setPass(true);
        return dto;
    }
}
