package com.school.portal.repository;

import com.school.portal.domain.entity.GradeScaleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GradeScaleEntryRepository extends JpaRepository<GradeScaleEntry, Long> {

    List<GradeScaleEntry> findByGradeScaleIdOrderByMinScoreDesc(Long gradeScaleId);

    /** Find the matching entry for a given raw score percentage */
    @Query("SELECT e FROM GradeScaleEntry e WHERE e.gradeScale.id = :scaleId "
         + "AND :score BETWEEN e.minScore AND e.maxScore")
    Optional<GradeScaleEntry> findMatchingEntry(Long scaleId, BigDecimal score);

    /** Check whether any grades reference entries of this scale */
    @Query("SELECT COUNT(g) > 0 FROM Grade g WHERE g.gradeScaleEntry.gradeScale.id = :scaleId")
    boolean isReferencedByGrades(Long scaleId);
}
