package com.krx2.employeedatamanagment.employee.dto;

import com.krx2.employeedatamanagment.employee.Gender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validRequestHasNoViolations() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "123-45-6789");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void blankFirstNameIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                " ", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "123-45-6789");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
    }

    @Test
    void futureDateOfBirthIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.now().plusDays(1), Gender.MALE, "123-45-6789");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth"));
    }

    @Test
    void malformedSsnIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "not-an-ssn");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void missingGenderIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), null, "123-45-6789");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("gender"));
    }
}
