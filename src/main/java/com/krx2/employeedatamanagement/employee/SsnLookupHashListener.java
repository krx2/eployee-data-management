package com.krx2.employeedatamanagement.employee;

import com.krx2.employeedatamanagement.crypto.SsnLookupHashService;
import jakarta.persistence.PrePersist;
import org.springframework.stereotype.Component;

@Component
public class SsnLookupHashListener {

    private final SsnLookupHashService ssnLookupHashService;

    public SsnLookupHashListener(SsnLookupHashService ssnLookupHashService) {
        this.ssnLookupHashService = ssnLookupHashService;
    }

    @PrePersist
    public void computeLookupHash(Employee employee) {
        employee.assignSsnLookupHash(ssnLookupHashService.hash(employee.ssnForInternalUseOnly()));
    }
}
