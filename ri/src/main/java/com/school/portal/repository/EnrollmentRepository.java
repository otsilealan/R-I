package com.school.portal.repository;

import com.school.portal.domain.entity.Enrollment;
import com.school.portal.domain.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);
}
