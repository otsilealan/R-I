package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "students")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "student_number", nullable = false, length = 20, unique = true)
    private String studentNumber;

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Business-key equality: studentNumber is the natural identifier
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student other = (Student) o;
        return Objects.equals(studentNumber, other.studentNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentNumber);
    }
}
