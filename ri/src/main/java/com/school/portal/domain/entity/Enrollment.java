package com.school.portal.domain.entity;

import com.school.portal.domain.enums.EnrollmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "enrollments")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class Enrollment {

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @CreatedDate
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private OffsetDateTime enrolledAt;

    /** Business key: student + course */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enrollment)) return false;
        Enrollment other = (Enrollment) o;
        return Objects.equals(student, other.student)
                && Objects.equals(course, other.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(student, course);
    }
}
