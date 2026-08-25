package com.school.portal.repository;

import com.school.portal.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmployeeNumber(String employeeNumber);
}
