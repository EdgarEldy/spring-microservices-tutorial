package com.edgareldy.springmicroservicestutorial.customerservice.service;

import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;

/**
 * Contract for managing {@link Customer} profiles exposed under
 * {@code /api/v1/customers}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface CustomerService {

    /** Creates a new customer profile for an already existing {@code userId}. */
    Customer create(CustomerRequest request);

    /** Looks a customer up by id, the endpoint {@code order-service} will call via Feign. */
    Customer findById(Long id);

    /** Updates every editable field of an existing customer; {@code userId} is left untouched. */
    Customer update(Long id, CustomerUpdateRequest request);

    /** Maps a {@link Customer} entity to its public {@link CustomerResponse} view. */
    CustomerResponse toResponse(Customer customer);
}
