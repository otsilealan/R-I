package com.school.portal.service;

import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.EnrollmentStatus;
import com.school.portal.dto.*;
import com.school.portal.exception.*;
import com.school.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExamResultService {

    private final ExamResultRepository    examResultRepository;
    private final GradeRepository         gradeRepository;
    private final CourseRepository        courseRepository;
    private final StudentRepository       studentRepository;
    private final EnrollmentRepository    enrollmentRepository;
    private final GradeScaleService       gradeScaleService;
    private final ExamResultMapper        mapper;

    /**
     * Submit a new exam result.
     * Validates: enrollment active, period open, score within range.
     * Persists ExamResult then resolves and persists Grade.
     */
    @Transactional
    public ExamResultDTO submitResult(SubmitResultRequest request, String submittedBy) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", request.getStudentId()));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        validateEnrollment(student, course);
        validatePeriodOpen(course);
        validateScoreRange(request.getRawScore(), course.getMaxScore());

        ExamResult result = new ExamResult();
        result.setStudent(student);
        result.setCourse(course);
        result.setExamName(request.getExamName());
        result.setRawScore(request.getRawScore());
        result.setMaxScore(course.getMaxScore());
        result.setCreatedBy(submittedBy);

        ExamResult saved = examResultRepository.save(result);
        Grade grade = resolveAndPersistGrade(saved, submittedBy);

        return mapper.toDTO(saved, grade);
    }

    /**
     * Submit a correction to an existing result.
     * Original row is never modified — a new row is created with supersedes pointing to original.
     */
    @Transactional
    public ExamResultDTO correctResult(Long originalId, CorrectResultRequest request,
                                       String correctedBy) {
        ExamResult original = examResultRepository.findById(originalId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamResult", originalId));

        validatePeriodOpen(original.getCourse());
        validateScoreRange(request.getRawScore(), original.getMaxScore());

        ExamResult correction = new ExamResult();
        correction.setStudent(original.getStudent());
        correction.setCourse(original.getCourse());
        correction.setExamName(request.getCorrectionReason() != null
                ? original.getExamName() + " [corrected: " + request.getCorrectionReason() + "]"
                : original.getExamName());
        correction.setRawScore(request.getRawScore());
        correction.setMaxScore(original.getMaxScore());
        correction.setSupersedes(original);
        correction.setCreatedBy(correctedBy);

        ExamResult saved = examResultRepository.save(correction);
        Grade grade = resolveAndPersistGrade(saved, correctedBy);

        return mapper.toDTO(saved, grade);
    }

    /** Returns a page of active (non-superseded) exam results with optional filters. */
    @Transactional(readOnly = true)
    public Page<ExamResultDTO> queryResults(Long studentId, Long courseId, Pageable pageable) {
        return examResultRepository.findActiveResults(studentId, courseId, pageable)
                .map(er -> {
                    Grade g = gradeRepository.findByExamResultId(er.getId()).orElse(null);
                    return mapper.toDTO(er, g);
                });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateEnrollment(Student student, Course course) {
        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                student.getId(), course.getId(), EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new BusinessRuleViolationException(
                    "Student " + student.getId() + " is not enrolled in course " + course.getId());
        }
    }

    private void validatePeriodOpen(Course course) {
        OffsetDateTime close = course.getReportingPeriod().getGradeSubmissionClose();
        if (OffsetDateTime.now().isAfter(close)) {
            throw new ClosedPeriodException(
                    course.getReportingPeriod().getName(), close.toString());
        }
    }

    private void validateScoreRange(BigDecimal score, BigDecimal maxScore) {
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
            throw new BusinessRuleViolationException(
                    "Score " + score + " is outside the valid range [0, " + maxScore + "].");
        }
    }

    private Grade resolveAndPersistGrade(ExamResult result, String resolvedBy) {
        // Convert raw score to percentage for scale lookup
        BigDecimal percentage = result.getRawScore()
                .divide(result.getMaxScore(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        // Use the first active grade scale (in a full system, scale would be
        // associated to the course or institution; this uses the first active scale as default)
        Grade grade = new Grade();
        grade.setExamResult(result);
        grade.setResolvedBy(resolvedBy);

        // Attempt to find a matching scale entry — fallback to UNGRADED
        Optional<GradeScaleEntry> entryOpt = gradeScaleService
                .resolveEntry(findActiveScaleId(), percentage);

        if (entryOpt.isPresent()) {
            GradeScaleEntry entry = entryOpt.get();
            grade.setGradeScaleEntry(entry);
            grade.setLetterGrade(entry.getLetterGrade());
            grade.setGradePoints(entry.getGradePoints());
        } else {
            // No matching entry — store a sentinel GradeScaleEntry reference
            // In production, a default "UNGRADED" entry should exist in the scale
            grade.setLetterGrade("UNGRADED");
            grade.setGradePoints(BigDecimal.ZERO);
            // grade.setGradeScaleEntry null is handled by allowing nullable FK in service layer
        }

        return gradeRepository.save(grade);
    }

    /** Returns the ID of the first active grade scale. Placeholder for course-level scale. */
    private Long findActiveScaleId() {
        return 1L; // In production, courses carry a grade_scale_id FK
    }
}
