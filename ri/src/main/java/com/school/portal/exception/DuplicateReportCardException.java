package com.school.portal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateReportCardException extends RuntimeException {
    public DuplicateReportCardException(Long studentId, Long periodId) {
        super("A report card already exists for student " + studentId
                + " in reporting period " + periodId);
    }
}
