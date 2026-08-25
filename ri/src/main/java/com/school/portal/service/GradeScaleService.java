package com.school.portal.service;

import com.school.portal.domain.entity.GradeScale;
import com.school.portal.domain.entity.GradeScaleEntry;
import com.school.portal.dto.CreateGradeScaleRequest;
import com.school.portal.dto.GradeScaleDTO;
import com.school.portal.dto.GradeScaleEntryDTO;
import com.school.portal.dto.GradeScaleMapper;
import com.school.portal.exception.BusinessRuleViolationException;
import com.school.portal.exception.ResourceNotFoundException;
import com.school.portal.repository.GradeScaleEntryRepository;
import com.school.portal.repository.GradeScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GradeScaleService {

    private final GradeScaleRepository gradeScaleRepository;
    private final GradeScaleEntryRepository gradeScaleEntryRepository;
    private final GradeScaleMapper mapper;

    /** Returns all active grade scales. */
    @Transactional(readOnly = true)
    public List<GradeScaleDTO> listActiveScales() {
        return gradeScaleRepository.findByActiveTrue()
                .stream().map(mapper::toDTO).toList();
    }

    /**
     * Creates a new grade scale.
     * Validates non-overlapping, non-duplicate letter grades before persisting.
     */
    @Transactional
    public GradeScaleDTO createGradeScale(CreateGradeScaleRequest request, String createdBy) {
        validateEntries(request.getEntries());

        GradeScale scale = new GradeScale();
        scale.setName(request.getName());
        scale.setDescription(request.getDescription());
        scale.setCreatedBy(createdBy);

        for (GradeScaleEntryDTO dto : request.getEntries()) {
            GradeScaleEntry entry = new GradeScaleEntry();
            entry.setLetterGrade(dto.getLetterGrade());
            entry.setMinScore(dto.getMinScore());
            entry.setMaxScore(dto.getMaxScore());
            entry.setGradePoints(dto.getGradePoints());
            entry.setPassIndicator(dto.isPass());
            entry.setDisplayOrder(dto.getDisplayOrder());
            scale.addEntry(entry);
        }

        return mapper.toDTO(gradeScaleRepository.save(scale));
    }

    /**
     * Deletes a grade scale. Rejects if any grade record references it.
     */
    @Transactional
    public void deleteGradeScale(Long id) {
        GradeScale scale = gradeScaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradeScale", id));

        if (gradeScaleEntryRepository.isReferencedByGrades(id)) {
            throw new BusinessRuleViolationException(
                    "Cannot delete grade scale '" + scale.getName()
                            + "' because it is referenced by existing grade records.");
        }
        gradeScaleRepository.delete(scale);
    }

    /**
     * Resolves the letter grade for a given percentage score against a scale.
     * Returns "UNGRADED" when no matching entry is found (FR-005 edge case).
     */
    @Transactional(readOnly = true)
    public String resolveLetterGrade(Long scaleId, BigDecimal score) {
        Optional<GradeScaleEntry> entry =
                gradeScaleEntryRepository.findMatchingEntry(scaleId, score);
        return entry.map(GradeScaleEntry::getLetterGrade).orElse("UNGRADED");
    }

    /**
     * Returns the matching GradeScaleEntry for a score, or empty when none matches.
     */
    @Transactional(readOnly = true)
    public Optional<GradeScaleEntry> resolveEntry(Long scaleId, BigDecimal score) {
        return gradeScaleEntryRepository.findMatchingEntry(scaleId, score);
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private void validateEntries(List<GradeScaleEntryDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new BusinessRuleViolationException("Grade scale must have at least one entry.");
        }
        // Sort by minScore ascending for overlap detection
        List<GradeScaleEntryDTO> sorted = entries.stream()
                .sorted((a, b) -> a.getMinScore().compareTo(b.getMinScore()))
                .toList();

        for (int i = 0; i < sorted.size() - 1; i++) {
            GradeScaleEntryDTO current = sorted.get(i);
            GradeScaleEntryDTO next    = sorted.get(i + 1);

            if (current.getMaxScore().compareTo(current.getMinScore()) <= 0) {
                throw new BusinessRuleViolationException(
                        "Entry '" + current.getLetterGrade()
                                + "': maxScore must be greater than minScore.");
            }
            // Overlap: current.maxScore >= next.minScore
            if (current.getMaxScore().compareTo(next.getMinScore()) >= 0) {
                throw new BusinessRuleViolationException(
                        "Grade scale entries overlap between '"
                                + current.getLetterGrade() + "' ("
                                + current.getMinScore() + "–" + current.getMaxScore()
                                + ") and '"
                                + next.getLetterGrade() + "' ("
                                + next.getMinScore() + "–" + next.getMaxScore() + ").");
            }
        }
    }
}
