package com.krx2.employeedatamanagement.employee;

import tools.jackson.databind.ObjectMapper;
import com.krx2.employeedatamanagement.common.EmployeeNotFoundException;
import com.krx2.employeedatamanagement.employee.dto.EmployeeCreateRequest;
import com.krx2.employeedatamanagement.employee.dto.EmployeeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createReturns201WithLocationAndNoPlaintextSsn() throws Exception {
        UUID id = UUID.randomUUID();
        EmployeeResponse response = new EmployeeResponse(
                id, "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "***-**-6789");
        given(employeeService.create(any())).willReturn(response);

        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "123-45-6789");

        mockMvc.perform(post("/employees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/employees/" + id))
                .andExpect(jsonPath("$.maskedSsn").value("***-**-6789"))
                .andExpect(content().string(not(containsString("123-45-6789"))));
    }

    @Test
    void createWithInvalidPayloadReturns400() throws Exception {
        String invalidPayload = """
                {"firstName":"","lastName":"Kowalski","dateOfBirth":"1990-01-01","gender":"MALE","socialSecurityNumber":"invalid"}
                """;

        mockMvc.perform(post("/employees")
                        .contentType("application/json")
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createWithInvalidEnumValueReturns400() throws Exception {
        String payload = """
                {"firstName":"Jan","lastName":"Kowalski","dateOfBirth":"1990-01-01","gender":"NOPE","socialSecurityNumber":"123-45-6789"}
                """;

        mockMvc.perform(post("/employees")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType("application/json")
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdWithNonUuidPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/employees/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        EmployeeResponse response = new EmployeeResponse(
                id, "Jan", "Kowalski", LocalDate.of(1990, 1, 1), Gender.MALE, "***-**-6789");
        given(employeeService.getById(id)).willReturn(response);

        mockMvc.perform(get("/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        given(employeeService.getById(id)).willThrow(new EmployeeNotFoundException(id));

        mockMvc.perform(get("/employees/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listReturns200() throws Exception {
        given(employeeService.list(any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk());
    }
}
