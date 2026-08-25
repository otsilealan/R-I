package com.school.portal.repository;

import com.school.portal.domain.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByReportingPeriodId(Long reportingPeriodId);
    List<Course> findByTeacherId(Long teacherId);
}
