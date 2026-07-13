package com.krx2.employeedatamanagement.employee.dto;

import com.krx2.employeedatamanagement.employee.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull @ReasonableDateOfBirth LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "\\d{3}-\\d{2}-\\d{4}", message = "must match format XXX-XX-XXXX")
        String socialSecurityNumber
) {
}
