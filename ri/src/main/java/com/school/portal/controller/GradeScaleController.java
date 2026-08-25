package com.school.portal.controller;

import com.school.portal.dto.CreateGradeScaleRequest;
import com.school.portal.dto.GradeScaleDTO;
import com.school.portal.service.GradeScaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/grade-scales")
@RequiredArgsConstructor
public class GradeScaleController {

    private final GradeScaleService gradeScaleService;

    @GetMapping
    public ResponseEntity<List<GradeScaleDTO>> listActiveScales() {
        return ResponseEntity.ok(gradeScaleService.listActiveScales());
    }

    @PostMapping
    public ResponseEntity<GradeScaleDTO> createGradeScale(
            @Valid @RequestBody CreateGradeScaleRequest request,
            Principal principal) {
        String createdBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gradeScaleService.createGradeScale(request, createdBy));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGradeScale(@PathVariable Long id) {
        gradeScaleService.deleteGradeScale(id);
        return ResponseEntity.noContent().build();
    }
}
