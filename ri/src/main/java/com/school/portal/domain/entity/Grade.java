package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "grades")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_result_id", nullable = false, unique = true)
    private ExamResult examResult;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_scale_entry_id", nullable = false)
    private GradeScaleEntry gradeScaleEntry;

    /** Snapshot of letter grade at resolution time */
    @NotBlank
    @Column(name = "letter_grade", nullable = false, length = 5)
    private String letterGrade;

    /** Snapshot of grade points at resolution time */
    @NotNull
    @Column(name = "grade_points", nullable = false, precision = 3, scale = 2)
    private BigDecimal gradePoints;

    @CreatedDate
    @Column(name = "resolved_at", nullable = false, updatable = false)
    private OffsetDateTime resolvedAt;

    @CreatedBy
    @Column(name = "resolved_by", nullable = false, length = 100)
    private String resolvedBy;

    /** Business key: examResult */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Grade)) return false;
        Grade other = (Grade) o;
        return Objects.equals(examResult, other.examResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(examResult);
    }
}
