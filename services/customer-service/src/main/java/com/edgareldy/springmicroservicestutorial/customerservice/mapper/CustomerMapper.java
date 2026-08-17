package com.edgareldy.springmicroservicestutorial.customerservice.mapper;

import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper translating between {@link Customer} entities and their request/response
 * DTOs. Plain interface, no extra collaborator needed: unlike {@code catalog-service}'s
 * {@code ProductMapper}, {@code userId} is already a plain {@code Long} on both
 * {@link Customer} and {@link CustomerRequest}, so there is no foreign entity to resolve.
 * <p>
 * {@code toEntity(CustomerRequest)}: every field maps directly, {@code id} is ignored (left
 * null for a new, unsaved entity).
 * <p>
 * {@code updateFromRequest(CustomerUpdateRequest, Customer)} mutates an already-managed
 * {@code Customer} in place via {@code @MappingTarget} rather than building a new entity:
 * {@link CustomerUpdateRequest} carries no {@code userId} field at all, so MapStruct would
 * never touch {@code Customer.userId} here even without the explicit {@code ignore = true}
 * below, matching the "immutable after creation" rule from that DTO's own Javadoc; {@code id}
 * and {@code userId} are still both listed explicitly so MapStruct's default
 * {@code unmappedTargetPolicy} (WARN) has nothing left to warn about on this method.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void updateFromRequest(CustomerUpdateRequest request, @MappingTarget Customer customer);
}
