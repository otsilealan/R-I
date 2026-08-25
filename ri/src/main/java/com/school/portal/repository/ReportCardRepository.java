package com.school.portal.repository;

import com.school.portal.domain.entity.ReportCard;
import com.school.portal.domain.enums.ReportCardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportCardRepository extends JpaRepository<ReportCard, Long> {
    Optional<ReportCard> findByStudentIdAndReportingPeriodId(Long studentId, Long periodId);
    boolean existsByStudentIdAndReportingPeriodIdAndStatus(Long studentId, Long periodId, ReportCardStatus status);
    Page<ReportCard> findByStudentId(Long studentId, Pageable pageable);
    Page<ReportCard> findByStudentIdAndReportingPeriodId(Long studentId, Long periodId, Pageable pageable);
}
