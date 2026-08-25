package com.school.portal.service;

import com.school.portal.dto.AggregateRefreshDTO;
import com.school.portal.dto.CourseAggregateDTO;
import com.school.portal.dto.StudentRankingDTO;
import com.school.portal.exception.ResourceNotFoundException;
import com.school.portal.repository.AggregateRepository;
import com.school.portal.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AggregateService {

    private final AggregateRepository aggregateRepository;
    private final CourseRepository    courseRepository;

    /**
     * Returns class-level aggregate statistics for a course.
     * Reads from mv_course_aggregates; exposes lastRefreshedAt from mv_refresh_log.
     */
    @Transactional(readOnly = true)
    public CourseAggregateDTO getCourseAggregates(Long courseId) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        List<StudentRankingDTO> rankings = aggregateRepository.findCourseAggregates(courseId);

        CourseAggregateDTO dto = new CourseAggregateDTO();
        dto.setCourseId(courseId);
        dto.setCourseName(course.getCourseName());
        dto.setStudentCount(aggregateRepository.countStudentsInCourse(courseId));
        dto.setClassAverage(aggregateRepository.findClassAverage(courseId).orElse(null));
        dto.setLastRefreshedAt(aggregateRepository.findLastRefreshedAt().orElse(null));
        dto.setStudentRankings(rankings);
        return dto;
    }

    /**
     * Triggers an explicit refresh of mv_course_aggregates and records the timestamp.
     * Refresh is always explicit — never silent (FR-015).
     */
    @Transactional
    public AggregateRefreshDTO refreshAggregates(String refreshedBy) {
        aggregateRepository.refreshMaterializedView();
        aggregateRepository.recordRefresh(refreshedBy);

        AggregateRefreshDTO dto = new AggregateRefreshDTO();
        dto.setRefreshedAt(OffsetDateTime.now());
        dto.setRefreshedBy(refreshedBy);
        return dto;
    }
}
