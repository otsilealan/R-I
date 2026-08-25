package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "reporting_periods")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class ReportingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Column(name = "grade_submission_close", nullable = false)
    private OffsetDateTime gradeSubmissionClose;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Business key: name + academicYear */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportingPeriod)) return false;
        ReportingPeriod other = (ReportingPeriod) o;
        return Objects.equals(name, other.name)
                && Objects.equals(academicYear, other.academicYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, academicYear);
    }
}
