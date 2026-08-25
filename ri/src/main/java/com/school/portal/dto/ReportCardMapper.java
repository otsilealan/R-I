package com.school.portal.dto;

import com.school.portal.domain.entity.ReportCard;
import com.school.portal.domain.entity.ReportCardItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReportCardMapper {

    @Mapping(target = "status",          expression = "java(rc.getStatus().name())")
    @Mapping(target = "student.id",              source = "rc.student.id")
    @Mapping(target = "student.studentNumber",   source = "rc.student.studentNumber")
    @Mapping(target = "student.firstName",       source = "rc.student.firstName")
    @Mapping(target = "student.lastName",        source = "rc.student.lastName")
    @Mapping(target = "student.dateOfBirth",     source = "rc.student.dateOfBirth")
    @Mapping(target = "reportingPeriod.id",          source = "rc.reportingPeriod.id")
    @Mapping(target = "reportingPeriod.name",         source = "rc.reportingPeriod.name")
    @Mapping(target = "reportingPeriod.academicYear", source = "rc.reportingPeriod.academicYear")
    @Mapping(target = "reportingPeriod.startDate",    source = "rc.reportingPeriod.startDate")
    @Mapping(target = "reportingPeriod.endDate",      source = "rc.reportingPeriod.endDate")
    @Mapping(target = "courseResults",   source = "items")
    @Mapping(target = "overallLetterGrade", ignore = true)  // computed by service
    ReportCardDTO toDTO(ReportCard rc, List<ReportCardItem> items);

    @Mapping(target = "courseId",   source = "item.course.id")
    @Mapping(target = "courseCode", source = "item.course.courseCode")
    @Mapping(target = "courseName", source = "item.course.courseName")
    @Mapping(target = "teacherName", expression = "java(item.getCourse().getTeacher().getFirstName() + ' ' + item.getCourse().getTeacher().getLastName())")
    @Mapping(target = "isIncomplete", source = "item.incomplete")
    @Mapping(target = "classRank",  ignore = true)  // populated by service from mv
    @Mapping(target = "classSize",  ignore = true)  // populated by service from mv
    CourseResultDTO itemToDTO(ReportCardItem item);
}
