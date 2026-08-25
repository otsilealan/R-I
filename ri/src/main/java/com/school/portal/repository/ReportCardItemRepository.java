package com.school.portal.repository;

import com.school.portal.domain.entity.ReportCardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportCardItemRepository extends JpaRepository<ReportCardItem, Long> {
    List<ReportCardItem> findByReportCardId(Long reportCardId);
}
