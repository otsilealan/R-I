package com.school.portal.controller;

import com.school.portal.dto.GenerateReportCardRequest;
import com.school.portal.dto.ReportCardDTO;
import com.school.portal.service.ReportCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/report-cards")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;

    @PostMapping("/generate")
    public ResponseEntity<ReportCardDTO> generate(
            @Valid @RequestBody GenerateReportCardRequest request,
            Principal principal) {
        String actor = principal != null ? principal.getName() : "system";
        ReportCardDTO dto = reportCardService.generateReportCard(
                request.getStudentId(), request.getReportingPeriodId(), actor);
        // 200 if existing draft returned, 201 if newly created
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReportCardDTO> approve(
            @PathVariable Long id,
            Principal principal) {
        String actor = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(reportCardService.approveReportCard(id, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportCardDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reportCardService.getReportCard(id));
    }

    @GetMapping
    public ResponseEntity<Page<ReportCardDTO>> list(
            @RequestParam Long studentId,
            @RequestParam(required = false) Long reportingPeriodId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                reportCardService.listReportCards(studentId, reportingPeriodId, pageable));
    }
}
