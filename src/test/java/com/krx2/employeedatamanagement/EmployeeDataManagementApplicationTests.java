package com.krx2.employeedatamanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.api-key=test-api-key",
        "app.ssn-lookup-pepper=test-ssn-lookup-pepper"
})
class EmployeeDataManagementApplicationTests {

    @Test
    void contextLoads() {
    }

}
