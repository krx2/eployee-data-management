package com.krx2.employeedatamanagement.employee.dto;

import com.krx2.employeedatamanagement.employee.Gender;
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
    void missingSsnIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, null);

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void ssnWithZeroAreaIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "000-12-3456");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void ssnWithReservedAreaSixSixSixIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "666-12-3456");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void ssnWithAreaInReservedNineHundredRangeIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "901-12-3456");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void ssnWithZeroGroupIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "123-00-4567");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void ssnWithZeroSerialIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "123-45-0000");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("socialSecurityNumber"));
    }

    @Test
    void unreasonablyOldDateOfBirthIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1700, 1, 1), Gender.MALE, "123-45-6789");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth"));
    }

    @Test
    void missingGenderIsRejected() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), null, "123-45-6789");

        Set<ConstraintViolation<EmployeeCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("gender"));
    }
}
