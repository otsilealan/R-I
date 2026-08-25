package com.school.portal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ClosedPeriodException extends RuntimeException {
    public ClosedPeriodException(String periodName, String closeDate) {
        super("Reporting period '" + periodName + "' is closed since " + closeDate);
    }
}
