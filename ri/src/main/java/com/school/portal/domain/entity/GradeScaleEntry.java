package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "grade_scale_entries")
@Getter @Setter @NoArgsConstructor
public class GradeScaleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_scale_id", nullable = false)
    private GradeScale gradeScale;

    @NotBlank
    @Column(name = "letter_grade", nullable = false, length = 5)
    private String letterGrade;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;

    @NotNull
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @NotNull
    @Column(name = "grade_points", nullable = false, precision = 3, scale = 2)
    private BigDecimal gradePoints;

    @Column(name = "pass_indicator", nullable = false)
    private boolean passIndicator = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /** Business key: gradeScale + letterGrade */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeScaleEntry)) return false;
        GradeScaleEntry other = (GradeScaleEntry) o;
        return Objects.equals(gradeScale, other.gradeScale)
                && Objects.equals(letterGrade, other.letterGrade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gradeScale, letterGrade);
    }
}
