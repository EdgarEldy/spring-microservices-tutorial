package com.edgareldy.springmicroservicestutorial.catalogservice.contract;

import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.catalogservice.config.MethodSecurityConfig;
import com.edgareldy.springmicroservicestutorial.catalogservice.controller.ProductController;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.ProductService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class the {@code spring-cloud-contract-maven-plugin} generates its JUnit test
 * against (one per file under {@code src/test/resources/contracts}), wired via this
 * module's pom {@code <baseClassForTests>}. Reuses {@code ProductControllerTest}'s own
 * {@code @WebMvcTest} slice pattern rather than a full {@code @SpringBootTest}: this test's
 * only job is proving {@link ProductController} really serializes what
 * {@code contracts/product/findProductById.yml} claims, not exercising the database,
 * Eureka registration, or config-server import that a full context would pull in.
 * {@link ProductService#findById(Long)} is stubbed with Mockito rather than backed by a
 * real database, exactly like {@code ProductControllerTest}; {@link JwtService} is mocked
 * purely so the security filter chain's beans are satisfiable, since {@code GET
 * /api/v1/catalog/products/{id}} is permitted to every caller regardless
 * ({@code SecurityConfig} {@code permitAll()}s it), so no JWT is ever presented or checked
 * by the generated test itself.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(ProductController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
public abstract class ProductContractBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Values match contracts/product/findProductById.yml's request/response body
        // exactly: the generated test replays the contract's request and asserts the
        // response against the contract's response, so a mismatch here would fail that
        // generated test, not this class.
        when(productService.findById(1L)).thenReturn(new ProductResponse(1L, "Clean Code", 39.90, 1L));

        RestAssuredMockMvc.mockMvc(mockMvc);
    }
}
