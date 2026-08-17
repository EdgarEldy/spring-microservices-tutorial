package com.edgareldy.springmicroservicestutorial.customerservice.service.impl;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;
import com.edgareldy.springmicroservicestutorial.customerservice.mapper.CustomerMapper;
import com.edgareldy.springmicroservicestutorial.customerservice.repository.CustomerRepository;
import com.edgareldy.springmicroservicestutorial.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CustomerService} implementation, backed by {@link CustomerRepository} and
 * delegating entity-DTO mapping in both directions to {@link CustomerMapper}. Every method
 * maps its entity to a {@link CustomerResponse} before returning, since nothing else needs
 * the raw {@link Customer} afterward. Deliberately never calls out to {@code auth-service}:
 * {@link #create} accepts the caller's {@code userId} as-is (see {@link CustomerRequest}'s
 * class Javadoc for why no Feign validation happens here), and {@link #update} never touches
 * it once a profile exists ({@link CustomerMapper#updateFromRequest} only ever sees
 * {@link CustomerUpdateRequest}, which carries no {@code userId} field to begin with).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email already in use: " + request.email());
        }
        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return customerMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = findEntityById(id);
        customerMapper.updateFromRequest(request, customer);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    private Customer findEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No customer found with id " + id));
    }
}
