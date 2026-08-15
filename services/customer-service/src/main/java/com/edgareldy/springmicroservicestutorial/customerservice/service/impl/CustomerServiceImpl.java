package com.edgareldy.springmicroservicestutorial.customerservice.service.impl;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;
import com.edgareldy.springmicroservicestutorial.customerservice.repository.CustomerRepository;
import com.edgareldy.springmicroservicestutorial.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CustomerService} implementation, backed by {@link CustomerRepository}.
 * Deliberately never calls out to {@code auth-service}: {@link #create} accepts the caller's
 * {@code userId} as-is (see {@link CustomerRequest}'s class Javadoc for why no Feign
 * validation happens here), and {@link #update} never touches it once a profile exists.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public Customer create(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email already in use: " + request.email());
        }
        Customer customer = Customer.builder()
                .userId(request.userId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .telephone(request.telephone())
                .email(request.email())
                .address(request.address())
                .build();
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No customer found with id " + id));
    }

    @Override
    @Transactional
    public Customer update(Long id, CustomerUpdateRequest request) {
        Customer customer = findById(id);
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setTelephone(request.telephone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        // userId is never reassigned here, see CustomerUpdateRequest's class Javadoc.
        return customerRepository.save(customer);
    }

    @Override
    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getTelephone(),
                customer.getEmail(),
                customer.getAddress());
    }
}
