package com.school.portal.service;

import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import com.school.portal.dto.*;
import com.school.portal.exception.*;
import com.school.portal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceTest {

    @Mock private ExamResultRepository examResultRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private GradeScaleService gradeScaleService;
    @Mock private ExamResultMapper mapper;

    private ExamResultService service;

    private Student student;
    private Course  course;
    private ReportingPeriod period;

    @BeforeEach
    void setUp() {
        service = new ExamResultService(examResultRepository, gradeRepository, courseRepository,
                studentRepository, enrollmentRepository, gradeScaleService, mapper);

        period = new ReportingPeriod();
        period.setId(1L);
        period.setName("Semester 1");
        period.setAcademicYear("2026-2027");
        period.setGradeSubmissionClose(OffsetDateTime.now().plusDays(30));

        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU-001");
        student.setFirstName("Jane");
        student.setLastName("Doe");

        course = new Course();
        course.setId(1L);
        course.setMaxScore(new BigDecimal("100.00"));
        course.setReportingPeriod(period);
    }

    @Test
    void submitResult_studentNotEnrolled_throwsBusinessRuleViolation() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(1L, 1L, EnrollmentStatus.ACTIVE))
                .thenReturn(false);

        SubmitResultRequest req = new SubmitResultRequest();
        req.setStudentId(1L); req.setCourseId(1L);
        req.setExamName("Midterm"); req.setRawScore(new BigDecimal("78.50"));

        assertThatThrownBy(() -> service.submitResult(req, "teacher"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not enrolled");
    }

    @Test
    void submitResult_closedPeriod_throwsClosedPeriodException() {
        period.setGradeSubmissionClose(OffsetDateTime.now().minusDays(1)); // closed

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(1L, 1L, EnrollmentStatus.ACTIVE))
                .thenReturn(true);

        SubmitResultRequest req = new SubmitResultRequest();
        req.setStudentId(1L); req.setCourseId(1L);
        req.setExamName("Final"); req.setRawScore(new BigDecimal("85.00"));

        assertThatThrownBy(() -> service.submitResult(req, "teacher"))
                .isInstanceOf(ClosedPeriodException.class);
    }

    @Test
    void submitResult_scoreAboveMax_throwsBusinessRuleViolation() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(1L, 1L, EnrollmentStatus.ACTIVE))
                .thenReturn(true);

        SubmitResultRequest req = new SubmitResultRequest();
        req.setStudentId(1L); req.setCourseId(1L);
        req.setExamName("Midterm"); req.setRawScore(new BigDecimal("105.00")); // > 100

        assertThatThrownBy(() -> service.submitResult(req, "teacher"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("outside the valid range");
    }

    @Test
    void correctResult_originalNotFound_throwsResourceNotFound() {
        when(examResultRepository.findById(99L)).thenReturn(Optional.empty());

        CorrectResultRequest req = new CorrectResultRequest();
        req.setRawScore(new BigDecimal("82.00"));

        assertThatThrownBy(() -> service.correctResult(99L, req, "teacher"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void correctResult_doesNotMutateOriginal() {
        ExamResult original = new ExamResult();
        original.setId(10L);
        original.setStudent(student);
        original.setCourse(course);
        original.setExamName("Midterm");
        original.setRawScore(new BigDecimal("78.50"));
        original.setMaxScore(new BigDecimal("100.00"));

        when(examResultRepository.findById(10L)).thenReturn(Optional.of(original));
        when(gradeScaleService.resolveEntry(anyLong(), any())).thenReturn(Optional.empty());
        when(examResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(), any())).thenReturn(new ExamResultDTO());

        CorrectResultRequest req = new CorrectResultRequest();
        req.setRawScore(new BigDecimal("82.00"));

        service.correctResult(10L, req, "teacher");

        // Original raw score must not have changed
        assertThat(original.getRawScore()).isEqualByComparingTo("78.50");

        // Saved correction must reference original
        verify(examResultRepository).save(argThat(
                saved -> saved.getSupersedes() != null
                      && saved.getSupersedes().getId().equals(10L)));
    }
}
