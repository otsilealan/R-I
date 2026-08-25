package com.school.portal.service;

import com.school.portal.domain.entity.*;
import com.school.portal.domain.enums.ReportCardStatus;
import com.school.portal.dto.*;
import com.school.portal.exception.ResourceNotFoundException;
import com.school.portal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock private ReportCardRepository     reportCardRepository;
    @Mock private ReportCardItemRepository reportCardItemRepository;
    @Mock private StudentRepository        studentRepository;
    @Mock private ReportingPeriodRepository periodRepository;
    @Mock private EnrollmentRepository     enrollmentRepository;
    @Mock private CourseRepository         courseRepository;
    @Mock private AggregateRepository      aggregateRepository;
    @Mock private ReportCardMapper         mapper;
    @Mock private JdbcTemplate             jdbc;

    private ReportCardService service;

    @BeforeEach
    void setUp() {
        service = new ReportCardService(reportCardRepository, reportCardItemRepository,
                studentRepository, periodRepository, enrollmentRepository, courseRepository,
                aggregateRepository, mapper, jdbc);
    }

    @Test
    void generateReportCard_studentNotFound_throwsResourceNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generateReportCard(99L, 1L, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateReportCard_existingDraft_returnsExistingWithoutSave() {
        Student s = new Student(); s.setId(1L);
        ReportingPeriod rp = new ReportingPeriod(); rp.setId(1L);
        ReportCard existing = new ReportCard();
        existing.setId(5L);
        existing.setStatus(ReportCardStatus.DRAFT);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(rp));
        when(reportCardRepository.findByStudentIdAndReportingPeriodId(1L, 1L))
                .thenReturn(Optional.of(existing));
        when(mapper.toDTO(any(), any())).thenReturn(new ReportCardDTO());

        service.generateReportCard(1L, 1L, "admin");
        // reportCardRepository.save should NOT be called (idempotency)
        verify(reportCardRepository, never()).save(any());
    }

    @Test
    void approveReportCard_alreadyApproved_throwsIllegalState() {
        ReportCard rc = new ReportCard();
        rc.setId(1L); rc.setStatus(ReportCardStatus.APPROVED);
        when(reportCardRepository.findById(1L)).thenReturn(Optional.of(rc));

        assertThatThrownBy(() -> service.approveReportCard(1L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already APPROVED");
    }

    @Test
    void approveReportCard_draft_transitionsToApproved() {
        ReportCard rc = new ReportCard();
        rc.setId(2L); rc.setStatus(ReportCardStatus.DRAFT);
        when(reportCardRepository.findById(2L)).thenReturn(Optional.of(rc));
        when(mapper.toDTO(any(), any())).thenReturn(new ReportCardDTO());

        service.approveReportCard(2L, "admin");
        assertThat(rc.getStatus()).isEqualTo(ReportCardStatus.APPROVED);
        assertThat(rc.getApprovedBy()).isEqualTo("admin");
        assertThat(rc.getApprovedAt()).isNotNull();
    }
}
