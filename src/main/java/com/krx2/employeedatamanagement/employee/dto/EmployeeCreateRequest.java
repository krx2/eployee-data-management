package com.krx2.employeedatamanagement.employee.dto;

import com.krx2.employeedatamanagement.employee.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull @ReasonableDateOfBirth LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @ValidSsn String socialSecurityNumber
) {
}
