package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "report_card_items")
@Getter @Setter @NoArgsConstructor
public class ReportCardItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_card_id", nullable = false)
    private ReportCard reportCard;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "course_average", precision = 5, scale = 2)
    private BigDecimal courseAverage;

    @Column(name = "letter_grade", length = 5)
    private String letterGrade;

    @Column(name = "grade_points", precision = 3, scale = 2)
    private BigDecimal gradePoints;

    @Column(name = "is_incomplete", nullable = false)
    private boolean incomplete = false;

    /** Business key: reportCard + course */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportCardItem)) return false;
        ReportCardItem other = (ReportCardItem) o;
        return Objects.equals(reportCard, other.reportCard)
                && Objects.equals(course, other.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportCard, course);
    }
}
