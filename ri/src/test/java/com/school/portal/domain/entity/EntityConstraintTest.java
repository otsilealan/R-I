package com.school.portal.domain.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T026 — Unit tests for Bean Validation constraints on core entities.
 * No database required — uses the Jakarta Validator directly.
 */
class EntityConstraintTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void studentRequiresStudentNumber() {
        Student s = new Student();
        s.setFirstName("Jane");
        s.setLastName("Doe");
        s.setDateOfBirth(LocalDate.of(2008, 3, 14));
        s.setEmail("jane@school.edu");
        // studentNumber intentionally blank
        s.setStudentNumber("");

        Set<ConstraintViolation<Student>> violations = validator.validate(s);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("studentNumber"));
    }

    @Test
    void studentRequiresValidEmail() {
        Student s = new Student();
        s.setStudentNumber("STU-001");
        s.setFirstName("Jane");
        s.setLastName("Doe");
        s.setDateOfBirth(LocalDate.of(2008, 3, 14));
        s.setEmail("not-an-email");

        Set<ConstraintViolation<Student>> violations = validator.validate(s);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validStudentPassesValidation() {
        Student s = new Student();
        s.setStudentNumber("STU-001");
        s.setFirstName("Jane");
        s.setLastName("Doe");
        s.setDateOfBirth(LocalDate.of(2008, 3, 14));
        s.setEmail("jane@school.edu");

        Set<ConstraintViolation<Student>> violations = validator.validate(s);
        assertThat(violations).isEmpty();
    }

    @Test
    void teacherRequiresEmployeeNumber() {
        Teacher t = new Teacher();
        t.setFirstName("Tom");
        t.setLastName("Smith");
        t.setEmail("tom@school.edu");
        t.setEmployeeNumber("");

        Set<ConstraintViolation<Teacher>> violations = validator.validate(t);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("employeeNumber"));
    }
}
