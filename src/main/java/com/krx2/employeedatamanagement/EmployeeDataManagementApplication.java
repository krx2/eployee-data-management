package com.krx2.employeedatamanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class EmployeeDataManagementApplication {

    static {
        // Bean Validation's default message interpolator falls back to the JVM/host default
        // locale when no Accept-Language is negotiated; pinning it here keeps API error
        // messages in English regardless of where the service happens to run.
        Locale.setDefault(Locale.ENGLISH);
    }

    public static void main(String[] args) {
        SpringApplication.run(EmployeeDataManagementApplication.class, args);
    }

}
