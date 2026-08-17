package com.edgareldy.springmicroservicestutorial.orderservice.mapper;

import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderRequest;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper translating between {@link Order} entities and their request/response
 * DTOs. {@code toResponse} is a pure field-for-field mapping. {@code toEntity} leaves three
 * fields for {@code OrderServiceImpl} to fill in after the Feign calls it makes them depend
 * on: {@code id} (a new, unsaved entity), {@code total} (computed as {@code quantity ×
 * unitPrice}, resolved via {@code ProductClient}, not something this mapper can know), and
 * {@code status} (left for {@link Order}'s own {@code @Builder.Default}, {@code PENDING}, to
 * apply, since not calling the builder's {@code status(...)} step at all is how Lombok's
 * {@code @Builder.Default} keeps its default value).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "status", ignore = true)
    Order toEntity(OrderRequest request);
}
