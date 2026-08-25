package com.school.portal.repository;

import com.school.portal.domain.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByExamResultId(Long examResultId);
}
