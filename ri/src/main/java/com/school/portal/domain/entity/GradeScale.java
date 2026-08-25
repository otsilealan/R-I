package com.school.portal.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "grade_scales")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class GradeScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "gradeScale", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minScore DESC")
    private List<GradeScaleEntry> entries = new ArrayList<>();

    public void addEntry(GradeScaleEntry entry) {
        entries.add(entry);
        entry.setGradeScale(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeScale)) return false;
        GradeScale other = (GradeScale) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
