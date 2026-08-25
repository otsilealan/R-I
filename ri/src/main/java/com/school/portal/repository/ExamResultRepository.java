package com.school.portal.repository;

import com.school.portal.domain.entity.ExamResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    /**
     * Returns only active (non-superseded) results for a student in a course.
     * Uses the v_active_exam_results view logic via a NOT EXISTS subquery.
     */
    @Query("SELECT er FROM ExamResult er WHERE er.student.id = :studentId "
         + "AND er.course.id = :courseId "
         + "AND NOT EXISTS (SELECT 1 FROM ExamResult newer WHERE newer.supersedes = er)")
    List<ExamResult> findActiveByStudentAndCourse(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId);

    @Query("SELECT CASE WHEN COUNT(er) > 0 THEN true ELSE false END "
         + "FROM ExamResult er WHERE er.student.id = :studentId "
         + "AND er.course.id = :courseId AND er.examName = :examName "
         + "AND NOT EXISTS (SELECT 1 FROM ExamResult newer WHERE newer.supersedes = er)")
    boolean existsActiveByStudentCourseAndExam(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("examName") String examName);

    /** Returns the full correction chain starting from the given result id */
    @Query("SELECT er FROM ExamResult er WHERE er.supersedes.id = :originalId")
    List<ExamResult> findSupersededChain(@Param("originalId") Long originalId);

    /** Pageable query over active results, optionally filtered */
    @Query("SELECT er FROM ExamResult er WHERE "
         + "(:studentId IS NULL OR er.student.id = :studentId) "
         + "AND (:courseId IS NULL OR er.course.id = :courseId) "
         + "AND NOT EXISTS (SELECT 1 FROM ExamResult newer WHERE newer.supersedes = er)")
    Page<ExamResult> findActiveResults(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            Pageable pageable);
}
