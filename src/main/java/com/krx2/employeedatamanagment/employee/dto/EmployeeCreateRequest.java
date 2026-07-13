package com.krx2.employeedatamanagment.employee.dto;

import com.krx2.employeedatamanagment.employee.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "\\d{3}-\\d{2}-\\d{4}", message = "must match format XXX-XX-XXXX")
        String socialSecurityNumber
) {
}
