package com.school.portal.service;

import com.school.portal.dto.StudentRankingDTO;
import com.school.portal.exception.ResourceNotFoundException;
import com.school.portal.repository.AggregateRepository;
import com.school.portal.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregateServiceTest {

    @Mock private AggregateRepository aggregateRepository;
    @Mock private CourseRepository courseRepository;

    private AggregateService service;

    @BeforeEach
    void setUp() {
        service = new AggregateService(aggregateRepository, courseRepository);
    }

    @Test
    void getCourseAggregates_courseNotFound_throwsResourceNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCourseAggregates(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCourseAggregates_noRankings_returnsEmptyList() {
        var course = new com.school.portal.domain.entity.Course();
        course.setId(1L); course.setCourseName("Math");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(aggregateRepository.findCourseAggregates(1L)).thenReturn(List.of());
        when(aggregateRepository.countStudentsInCourse(1L)).thenReturn(0);
        when(aggregateRepository.findClassAverage(1L)).thenReturn(Optional.empty());
        when(aggregateRepository.findLastRefreshedAt()).thenReturn(Optional.empty());

        var dto = service.getCourseAggregates(1L);
        assertThat(dto.getStudentRankings()).isEmpty();
        assertThat(dto.getClassAverage()).isNull();
        assertThat(dto.getLastRefreshedAt()).isNull();
    }

    @Test
    void getCourseAggregates_lastRefreshedAtPreservedAfterCorrection() {
        // Simulates: score corrected, but refresh not triggered → timestamp unchanged
        OffsetDateTime fixedRefresh = OffsetDateTime.now().minusHours(2);
        var course = new com.school.portal.domain.entity.Course();
        course.setId(2L); course.setCourseName("English");
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(aggregateRepository.findCourseAggregates(2L)).thenReturn(List.of());
        when(aggregateRepository.countStudentsInCourse(2L)).thenReturn(0);
        when(aggregateRepository.findClassAverage(2L)).thenReturn(Optional.of(BigDecimal.valueOf(75)));
        when(aggregateRepository.findLastRefreshedAt()).thenReturn(Optional.of(fixedRefresh));

        var dto = service.getCourseAggregates(2L);
        assertThat(dto.getLastRefreshedAt()).isEqualTo(fixedRefresh);
        // refreshMaterializedView was never called — no implicit refresh
        verify(aggregateRepository, never()).refreshMaterializedView();
    }

    @Test
    void refreshAggregates_callsRefreshAndRecords() {
        var dto = service.refreshAggregates("admin");
        verify(aggregateRepository).refreshMaterializedView();
        verify(aggregateRepository).recordRefresh("admin");
        assertThat(dto.getRefreshedBy()).isEqualTo("admin");
        assertThat(dto.getRefreshedAt()).isNotNull();
    }
}
