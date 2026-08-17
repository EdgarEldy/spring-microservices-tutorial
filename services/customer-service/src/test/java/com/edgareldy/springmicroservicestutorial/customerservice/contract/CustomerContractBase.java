package com.edgareldy.springmicroservicestutorial.customerservice.contract;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.customerservice.controller.CustomerController;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.customerservice.service.CustomerService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class spring-cloud-contract-maven-plugin generates one JUnit test per contract
 * against (see {@code contracts/customer/shouldReturnCustomerById.yml}), wiring
 * {@link MockMvc} to {@link RestAssuredMockMvc} so the generated tests can drive requests
 * through {@code CustomerController} exactly as the real servlet stack would, without a
 * running server. Deliberately a {@code @WebMvcTest} slice rather than a full
 * {@code @SpringBootTest}: this contract only needs to prove {@link CustomerController}'s
 * actual JSON serialization matches what the contract promises, not that the whole
 * application (real Postgres via Testcontainers, Eureka, config-server import, and so on)
 * boots, which would make `mvn install` on this module depend on Docker being available.
 * {@link CustomerService} is mocked so this stays a pure controller/serialization test, the
 * same reasoning {@link com.edgareldy.springmicroservicestutorial.customerservice.controller.CustomerControllerTest}
 * already applies for its own MockMvc slice tests.
 * <p>
 * Security is bypassed here on purpose, via {@code @AutoConfigureMockMvc(addFilters =
 * false)}, rather than importing the real {@code SecurityConfig} or modeling a fake JWT:
 * this contract only verifies response shape (id/name/email/etc. fields and the JSON
 * envelope), not the {@code 401 Unauthorized} failure path, which the README documents as
 * out of scope for contract testing and stays covered by
 * {@code CustomerControllerTest#findByIdReturnsUnauthorizedWithoutAuthentication} instead.
 * {@link JwtService} is still mocked here purely so {@code JwtAuthFilter} (auto-included by
 * {@code @WebMvcTest} because it is a servlet {@code Filter} bean) can be constructed; with
 * {@code addFilters = false} it never actually runs against these requests either way.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
public abstract class CustomerContractBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setup() {
        // Values must match contracts/customer/shouldReturnCustomerById.yml's response body
        // exactly (except `timestamp`, which the contract matches with a regex instead of a
        // literal since ApiResponse.success(...) stamps Instant.now() at call time).
        when(customerService.findById(eq(1L))).thenReturn(
                new CustomerResponse(1L, 1L, "Ada", "Lovelace", "+1234567890", "ada@example.com", "123 Main St"));

        RestAssuredMockMvc.mockMvc(mockMvc);
    }
}
