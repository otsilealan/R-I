package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "courses")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @NotBlank
    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriod reportingPeriod;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @NotNull
    @DecimalMin("0.01")
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore = new BigDecimal("100.00");

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Business key: courseCode + reportingPeriod */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course other = (Course) o;
        return Objects.equals(courseCode, other.courseCode)
                && Objects.equals(reportingPeriod, other.reportingPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode, reportingPeriod);
    }
}
