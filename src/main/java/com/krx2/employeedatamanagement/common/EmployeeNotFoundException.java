package com.krx2.employeedatamanagement.common;

import java.util.UUID;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(UUID id) {
        super("Employee not found: " + id);
    }
}
