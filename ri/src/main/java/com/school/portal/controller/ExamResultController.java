package com.school.portal.controller;

import com.school.portal.dto.*;
import com.school.portal.service.AggregateService;
import com.school.portal.service.ExamResultService;
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
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ExamResultController {

    private final ExamResultService examResultService;
    private final AggregateService  aggregateService;

    @PostMapping
    public ResponseEntity<ExamResultDTO> submitResult(
            @Valid @RequestBody SubmitResultRequest request,
            Principal principal) {
        String actor = principal != null ? principal.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examResultService.submitResult(request, actor));
    }

    @PostMapping("/{id}/correct")
    public ResponseEntity<ExamResultDTO> correctResult(
            @PathVariable Long id,
            @Valid @RequestBody CorrectResultRequest request,
            Principal principal) {
        String actor = principal != null ? principal.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examResultService.correctResult(id, request, actor));
    }

    @GetMapping
    public ResponseEntity<Page<ExamResultDTO>> queryResults(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(examResultService.queryResults(studentId, courseId, pageable));
    }

    @GetMapping("/aggregates")
    public ResponseEntity<CourseAggregateDTO> getCourseAggregates(
            @RequestParam Long courseId) {
        return ResponseEntity.ok(aggregateService.getCourseAggregates(courseId));
    }

    @PostMapping("/aggregates/refresh")
    public ResponseEntity<AggregateRefreshDTO> refreshAggregates(Principal principal) {
        String actor = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(aggregateService.refreshAggregates(actor));
    }
}
