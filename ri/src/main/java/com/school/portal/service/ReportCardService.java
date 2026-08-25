package com.school.portal.service;

import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.ReportCardStatus;
import com.school.portal.dto.*;
import com.school.portal.exception.ResourceNotFoundException;
import com.school.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final ReportCardRepository     reportCardRepository;
    private final ReportCardItemRepository reportCardItemRepository;
    private final StudentRepository        studentRepository;
    private final ReportingPeriodRepository periodRepository;
    private final EnrollmentRepository     enrollmentRepository;
    private final CourseRepository         courseRepository;
    private final AggregateRepository      aggregateRepository;
    private final ReportCardMapper         mapper;
    private final JdbcTemplate             jdbc;

    /**
     * Generate (or return existing draft) report card for a student and period.
     * Idempotent: returns existing DRAFT unchanged if one exists (FR-016).
     */
    @Transactional
    public ReportCardDTO generateReportCard(Long studentId, Long periodId, String generatedBy) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        ReportingPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportingPeriod", periodId));

        // Idempotency: return existing draft
        Optional<ReportCard> existing = reportCardRepository
                .findByStudentIdAndReportingPeriodId(studentId, periodId);
        if (existing.isPresent()) {
            ReportCard rc = existing.get();
            return mapper.toDTO(rc, rc.getItems());
        }

        // Build report card from v_student_period_summary
        List<Map<String, Object>> summary = jdbc.queryForList(
                "SELECT course_id, course_average, letter_grade, grade_points, is_incomplete "
              + "FROM v_student_period_summary "
              + "WHERE student_id = ? AND reporting_period_id = ?",
                studentId, periodId);

        ReportCard rc = new ReportCard();
        rc.setStudent(student);
        rc.setReportingPeriod(period);
        rc.setStatus(ReportCardStatus.DRAFT);
        rc.setGeneratedBy(generatedBy);

        List<ReportCardItem> items = new ArrayList<>();
        boolean anyIncomplete = false;
        BigDecimal totalAvg = BigDecimal.ZERO;
        int completedCount = 0;

        for (Map<String, Object> row : summary) {
            Long courseId = ((Number) row.get("course_id")).longValue();
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
            boolean incomplete = Boolean.TRUE.equals(row.get("is_incomplete"));

            ReportCardItem item = new ReportCardItem();
            item.setCourse(course);
            item.setIncomplete(incomplete);

            if (!incomplete) {
                BigDecimal avg = (BigDecimal) row.get("course_average");
                item.setCourseAverage(avg);
                item.setLetterGrade((String) row.get("letter_grade"));
                Object gp = row.get("grade_points");
                item.setGradePoints(gp != null ? (BigDecimal) gp : null);
                totalAvg = totalAvg.add(avg != null ? avg : BigDecimal.ZERO);
                completedCount++;
            } else {
                anyIncomplete = true;
            }
            items.add(item);
            rc.addItem(item);
        }

        rc.setHasIncompleteCourses(anyIncomplete);
        if (completedCount > 0) {
            rc.setOverallAverage(totalAvg.divide(
                    BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP));
        }

        ReportCard saved = reportCardRepository.save(rc);
        return mapper.toDTO(saved, saved.getItems());
    }

    /**
     * Approve a DRAFT report card. Transitions DRAFT → APPROVED.
     */
    @Transactional
    public ReportCardDTO approveReportCard(Long id, String approvedBy) {
        ReportCard rc = reportCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportCard", id));
        if (rc.getStatus() == ReportCardStatus.APPROVED) {
            throw new IllegalStateException("Report card " + id + " is already APPROVED.");
        }
        rc.setStatus(ReportCardStatus.APPROVED);
        rc.setApprovedAt(OffsetDateTime.now());
        rc.setApprovedBy(approvedBy);
        return mapper.toDTO(rc, rc.getItems());
    }

    /** Fetch a report card by id regardless of status. */
    @Transactional(readOnly = true)
    public ReportCardDTO getReportCard(Long id) {
        ReportCard rc = reportCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportCard", id));
        return mapper.toDTO(rc, rc.getItems());
    }

    /** List report cards for a student, optionally filtered by period. */
    @Transactional(readOnly = true)
    public Page<ReportCardDTO> listReportCards(Long studentId, Long periodId, Pageable pageable) {
        Page<ReportCard> page = periodId != null
                ? reportCardRepository.findByStudentIdAndReportingPeriodId(studentId, periodId, pageable)
                : reportCardRepository.findByStudentId(studentId, pageable);
        return page.map(rc -> mapper.toDTO(rc, rc.getItems()));
    }
}
