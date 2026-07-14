package com.krx2.employeedatamanagement.employee;

import com.krx2.employeedatamanagement.common.EmployeeNotFoundException;
import com.krx2.employeedatamanagement.employee.dto.EmployeeCreateRequest;
import com.krx2.employeedatamanagement.employee.dto.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository, EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    public EmployeeResponse create(EmployeeCreateRequest request) {
        // saveAndFlush forces the INSERT (and the ssn_lookup_hash uniqueness check) to happen
        // here, inside the repository call, so a duplicate SSN surfaces as a translated
        // DataIntegrityViolationException rather than only at end-of-transaction commit.
        Employee saved = employeeRepository.saveAndFlush(employeeMapper.toEntity(request));
        return employeeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(employeeMapper::toResponse);
    }

    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
    }
}
