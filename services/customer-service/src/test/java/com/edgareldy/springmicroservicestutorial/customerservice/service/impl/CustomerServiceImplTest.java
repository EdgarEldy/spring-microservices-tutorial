package com.edgareldy.springmicroservicestutorial.customerservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;
import com.edgareldy.springmicroservicestutorial.customerservice.mapper.CustomerMapperImpl;
import com.edgareldy.springmicroservicestutorial.customerservice.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for {@link CustomerServiceImpl}: no Spring context,
 * no database, {@link CustomerRepository} is mocked so only this class' own
 * branching logic is exercised (persistence itself is already covered by
 * {@code CustomerRepositoryTest}, backed by Testcontainers PostgreSQL). The
 * MapStruct-generated {@link CustomerMapperImpl} is instantiated for real
 * (not mocked) rather than via {@code @InjectMocks}: mocking a trivial
 * generated mapper would add nothing and would require stubbing every field
 * of every {@code toResponse} call.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, new CustomerMapperImpl());
    }

    private static CustomerRequest newRequest() {
        return new CustomerRequest(1L, "Ada", "Lovelace", "+1234567890", "ada@example.com", "123 Main St");
    }

    @Test
    void create_newEmail_persistsAndReturnsCustomer() {
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse created = customerService.create(newRequest());

        assertThat(created.userId()).isEqualTo(1L);
        assertThat(created.firstName()).isEqualTo("Ada");
        assertThat(created.email()).isEqualTo("ada@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_duplicateEmail_throwsBusinessRuleExceptionAndNeverSaves() {
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> customerService.create(newRequest()));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void findById_found_returnsCustomerResponse() {
        Customer customer = Customer.builder()
                .id(1L)
                .userId(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .telephone("+1234567890")
                .email("ada@example.com")
                .address("123 Main St")
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> customerService.findById(99L));
    }

    @Test
    void update_existingCustomer_updatesEditableFieldsAndLeavesUserIdUntouched() {
        Customer customer = Customer.builder()
                .id(1L)
                .userId(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .telephone("+1234567890")
                .email("ada@example.com")
                .address("123 Main St")
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerUpdateRequest request =
                new CustomerUpdateRequest("Grace", "Hopper", "+1987654321", "grace@example.com", "456 Oak Ave");
        CustomerResponse updated = customerService.update(1L, request);

        assertThat(updated.firstName()).isEqualTo("Grace");
        assertThat(updated.lastName()).isEqualTo("Hopper");
        assertThat(updated.email()).isEqualTo("grace@example.com");
        assertThat(updated.userId()).isEqualTo(1L);
        verify(customerRepository).save(customer);
    }

    @Test
    void update_notFound_throwsResourceNotFoundExceptionAndNeverSaves() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        CustomerUpdateRequest request =
                new CustomerUpdateRequest("Grace", "Hopper", "+1987654321", "grace@example.com", "456 Oak Ave");

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> customerService.update(99L, request));

        verify(customerRepository, never()).save(any());
    }
}
