package com.edgareldy.springmicroservicestutorial.customerservice.service;

import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;

/**
 * Contract for managing {@link Customer} profiles exposed under
 * {@code /api/v1/customers}. Every method returns {@link CustomerResponse} directly rather
 * than the {@link Customer} entity: nothing outside this service ever needs the raw entity
 * after a call, so there is no reason to make every caller repeat a {@code toResponse(...)}
 * call, the same reasoning {@code catalog-service}'s {@code CategoryService}/
 * {@code ProductService} already apply.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public interface CustomerService {

    /** Creates a new customer profile for an already existing {@code userId}. */
    CustomerResponse create(CustomerRequest request);

    /** Looks a customer up by id, the endpoint {@code order-service} will call via Feign. */
    CustomerResponse findById(Long id);

    /** Updates every editable field of an existing customer; {@code userId} is left untouched. */
    CustomerResponse update(Long id, CustomerUpdateRequest request);
}
