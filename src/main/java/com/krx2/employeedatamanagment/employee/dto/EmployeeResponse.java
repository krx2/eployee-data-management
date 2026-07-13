package com.krx2.employeedatamanagment.employee.dto;

import com.krx2.employeedatamanagment.employee.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String maskedSsn
) {
}
