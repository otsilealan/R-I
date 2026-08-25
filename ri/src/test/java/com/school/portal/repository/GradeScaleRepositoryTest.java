package com.school.portal.repository;

import com.school.portal.AbstractIntegrationTest;
import com.school.portal.domain.entity.GradeScale;
import com.school.portal.domain.entity.GradeScaleEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class GradeScaleRepositoryTest extends AbstractIntegrationTest {

    @Autowired private GradeScaleRepository gradeScaleRepository;
    @Autowired private GradeScaleEntryRepository entryRepository;

    @Test
    void persistAndRetrieveGradeScale() {
        GradeScale scale = buildScale("Test Scale");
        GradeScale saved = gradeScaleRepository.save(scale);

        assertThat(saved.getId()).isNotNull();
        assertThat(gradeScaleRepository.findById(saved.getId()))
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.getName()).isEqualTo("Test Scale"));
    }

    @Test
    void findMatchingEntry_returnsCorrectGrade() {
        GradeScale scale = gradeScaleRepository.save(buildScale("Find Scale"));

        // Score 85 should match B (80–89.99)
        var entry = entryRepository.findMatchingEntry(
                scale.getId(), new BigDecimal("85.00"));
        assertThat(entry).isPresent()
                .hasValueSatisfying(e -> assertThat(e.getLetterGrade()).isEqualTo("B"));
    }

    @Test
    void findMatchingEntry_noMatch_returnsEmpty() {
        GradeScale scale = gradeScaleRepository.save(buildScale("Sparse Scale 2"));
        // Score 50 — below F (60–69.99) — no match
        var entry = entryRepository.findMatchingEntry(
                scale.getId(), new BigDecimal("50.00"));
        assertThat(entry).isEmpty();
    }

    // -------------------------------------------------------------------------
    private GradeScale buildScale(String name) {
        GradeScale scale = new GradeScale();
        scale.setName(name);
        scale.setCreatedBy("test");

        scale.addEntry(makeEntry("A", "90.00", "100.00", "4.00", 1));
        scale.addEntry(makeEntry("B", "80.00",  "89.99", "3.00", 2));
        scale.addEntry(makeEntry("C", "70.00",  "79.99", "2.00", 3));
        scale.addEntry(makeEntry("D", "60.00",  "69.99", "1.00", 4));
        return scale;
    }

    private GradeScaleEntry makeEntry(String letter, String min, String max,
                                      String pts, int order) {
        GradeScaleEntry e = new GradeScaleEntry();
        e.setLetterGrade(letter);
        e.setMinScore(new BigDecimal(min));
        e.setMaxScore(new BigDecimal(max));
        e.setGradePoints(new BigDecimal(pts));
        e.setPassIndicator(!"F".equals(letter));
        e.setDisplayOrder(order);
        return e;
    }
}
