package com.school.portal.repository;

import com.school.portal.domain.entity.ReportingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportingPeriodRepository extends JpaRepository<ReportingPeriod, Long> {
    Optional<ReportingPeriod> findByNameAndAcademicYear(String name, String academicYear);
}
