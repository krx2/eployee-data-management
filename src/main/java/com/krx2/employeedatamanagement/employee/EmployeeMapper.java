package com.krx2.employeedatamanagement.employee;

import com.krx2.employeedatamanagement.crypto.SsnMasker;
import com.krx2.employeedatamanagement.employee.dto.EmployeeCreateRequest;
import com.krx2.employeedatamanagement.employee.dto.EmployeeResponse;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeCreateRequest request) {
        return new Employee(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.gender(),
                request.socialSecurityNumber()
        );
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDateOfBirth(),
                employee.getGender(),
                SsnMasker.mask(employee.ssnForInternalUseOnly())
        );
    }
}
