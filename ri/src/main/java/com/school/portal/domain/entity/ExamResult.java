package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Append-only exam result record.
 * Corrections are new rows referencing the original via supersedes.
 * No row may be deleted or updated once persisted (constitution Principle IV).
 */
@Entity
@Table(name = "exam_results")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotBlank
    @Column(name = "exam_name", nullable = false, length = 200)
    private String examName;

    /** Raw score — NUMERIC(5,2) per constitution Principle IV. No float allowed. */
    @NotNull
    @DecimalMin("0.00")
    @Column(name = "raw_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal rawScore;

    @NotNull
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    /** Points to the row this row supersedes; null for original submissions. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_id")
    private ExamResult supersedes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    /** Business key: student + course + examName + createdAt */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamResult)) return false;
        ExamResult other = (ExamResult) o;
        return Objects.equals(student, other.student)
                && Objects.equals(course, other.course)
                && Objects.equals(examName, other.examName)
                && Objects.equals(createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(student, course, examName, createdAt);
    }
}
