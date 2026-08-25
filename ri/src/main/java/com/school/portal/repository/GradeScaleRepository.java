package com.school.portal.repository;

import com.school.portal.domain.entity.GradeScale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GradeScaleRepository extends JpaRepository<GradeScale, Long> {
    List<GradeScale> findByActiveTrue();
    Optional<GradeScale> findByName(String name);
    boolean existsByName(String name);
}
