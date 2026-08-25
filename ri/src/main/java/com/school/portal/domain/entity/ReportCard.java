package com.school.portal.domain.entity;

import com.school.portal.domain.enums.ReportCardStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "report_cards")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class ReportCard {

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
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriod reportingPeriod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    @Column(name = "overall_average", precision = 5, scale = 2)
    private BigDecimal overallAverage;

    @Column(name = "overall_grade_points", precision = 3, scale = 2)
    private BigDecimal overallGradePoints;

    @Column(name = "has_incomplete_courses", nullable = false)
    private boolean hasIncompleteCourses = false;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "generated_by", nullable = false, length = 100)
    private String generatedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @OneToMany(mappedBy = "reportCard", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportCardItem> items = new ArrayList<>();

    public void addItem(ReportCardItem item) {
        items.add(item);
        item.setReportCard(this);
    }

    /** Business key: student + reportingPeriod */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportCard)) return false;
        ReportCard other = (ReportCard) o;
        return Objects.equals(student, other.student)
                && Objects.equals(reportingPeriod, other.reportingPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(student, reportingPeriod);
    }
}
