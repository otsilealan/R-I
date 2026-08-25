package com.school.portal.dto;

import com.school.portal.domain.entity.ExamResult;
import com.school.portal.domain.entity.Grade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamResultMapper {

    @Mapping(target = "studentId",   source = "result.student.id")
    @Mapping(target = "studentName", expression = "java(result.getStudent().getFirstName() + ' ' + result.getStudent().getLastName())")
    @Mapping(target = "courseId",    source = "result.course.id")
    @Mapping(target = "courseName",  source = "result.course.courseName")
    @Mapping(target = "supersedesId", expression = "java(result.getSupersedes() != null ? result.getSupersedes().getId() : null)")
    @Mapping(target = "letterGrade", source = "grade.letterGrade")
    @Mapping(target = "gradePoints", source = "grade.gradePoints")
    ExamResultDTO toDTO(ExamResult result, Grade grade);
}
