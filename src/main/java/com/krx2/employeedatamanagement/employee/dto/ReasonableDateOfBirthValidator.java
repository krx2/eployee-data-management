package com.krx2.employeedatamanagement.employee.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class ReasonableDateOfBirthValidator implements ConstraintValidator<ReasonableDateOfBirth, LocalDate> {

    private static final LocalDate EARLIEST_REASONABLE_DATE = LocalDate.of(1900, 1, 1);

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !value.isBefore(EARLIEST_REASONABLE_DATE) && value.isBefore(LocalDate.now().plusDays(1));
    }
}
