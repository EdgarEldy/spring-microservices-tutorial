package com.edgareldy.springmicroservicestutorial.orderservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.orderservice.client.CustomerClient;
import com.edgareldy.springmicroservicestutorial.orderservice.client.ProductClient;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderDetailResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderRequest;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.IdempotencyKey;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.Order;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.OrderStatus;
import com.edgareldy.springmicroservicestutorial.orderservice.event.OrderEventProducer;
import com.edgareldy.springmicroservicestutorial.orderservice.mapper.OrderMapperImpl;
import com.edgareldy.springmicroservicestutorial.orderservice.repository.IdempotencyKeyRepository;
import com.edgareldy.springmicroservicestutorial.orderservice.repository.OrderRepository;
import feign.Request;
import feign.RequestTemplate;
import feign.FeignException;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Pure Mockito unit tests for {@link OrderServiceImpl}: {@link OrderRepository}/
 * {@link IdempotencyKeyRepository}/{@link ProductClient}/{@link CustomerClient}/
 * {@link OrderEventProducer} are mocked, no Spring application context is started. The
 * MapStruct-generated {@link OrderMapperImpl} is instantiated for real (not mocked).
 * <p>
 * <strong>How the publish-after-commit test works</strong> (the single most important thing
 * tested in this class, per this branch's README task list): {@code create()} does not call
 * {@link OrderEventProducer} directly, it calls
 * {@code TransactionSynchronizationManager.registerSynchronization(...)} with an
 * {@code afterCommit()} callback, so the producer is only ever reached once whatever real
 * {@code PlatformTransactionManager} is driving the surrounding {@code @Transactional} method
 * decides to commit. Same technique as {@code auth-service}'s {@code AuthServiceImplTest}:
 * {@code TransactionSynchronizationManager.initSynchronization()} before calling
 * {@code create()} (making {@code isSynchronizationActive()} return {@code true}, the same
 * condition a real {@code @Transactional} proxy would have satisfied), assert the producer has
 * not been invoked yet immediately after {@code create()} returns (proving the call is
 * deferred, not immediate), then manually invoke {@code afterCommit()} on every registered
 * synchronization (simulating what a real transaction manager does once it actually commits)
 * and assert the producer is invoked exactly once, only then. A companion test exercises the
 * rollback path instead ({@code afterCompletion(STATUS_ROLLED_BACK)}), proving the producer is
 * never invoked at all when the transaction never commits.
 * <p>
 * {@code OrderServiceImpl.self} (its own {@code @Lazy}-injected proxy reference, used so
 * {@code create()} can invoke {@code createAndPersist()} through the real Spring proxy rather
 * than via a plain {@code this} call) is wired here via {@link ReflectionTestUtils#setField}
 * to point back at the very instance under test: a pure Mockito test constructs a plain object,
 * never a Spring-managed proxy, so there is no separate proxy instance to inject, the object
 * simply refers to itself.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository, idempotencyKeyRepository, new OrderMapperImpl(),
                productClient, customerClient, orderEventProducer, null);
        ReflectionTestUtils.setField(orderService, "self", orderService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static OrderRequest newRequest() {
        return new OrderRequest(1L, 1L, 2);
    }

    private static ProductResponse product(double unitPrice) {
        return new ProductResponse(1L, "Clean Code", unitPrice, 1L);
    }

    private static CustomerResponse customer() {
        return new CustomerResponse(1L, 1L, "Ada", "Lovelace", "+1234567890", "ada@example.com", "123 Main St");
    }

    private static FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }

    private static FeignException.ServiceUnavailable serviceUnavailable() {
        Request request = Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.ServiceUnavailable("unavailable", request, null, null);
    }

    @Test
    void create_newIdempotencyKey_validatesResolvesTotalAndPersists() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));
        when(productClient.getProduct(1L)).thenReturn(ApiResponse.success(product(39.90), "ok"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });

        TransactionSynchronizationManager.initSynchronization();
        OrderResponse response = orderService.create(newRequest(), "key-1");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.total()).isEqualTo(79.80);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void create_existingIdempotencyKey_returnsExistingOrderAndNeverRevalidates() {
        Order existingOrder = Order.builder().id(5L).customerId(1L).productId(1L).quantity(2).total(79.80).build();
        IdempotencyKey existingKey = IdempotencyKey.builder().idempotencyKey("key-1").order(existingOrder).build();
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existingKey));

        OrderResponse response = orderService.create(newRequest(), "key-1");

        assertThat(response.id()).isEqualTo(5L);
        verify(productClient, never()).getProduct(any());
        verify(customerClient, never()).getCustomer(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_productNotFound_throwsResourceNotFoundExceptionAndNeverSaves() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));
        when(productClient.getProduct(1L)).thenThrow(notFound());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> orderService.create(newRequest(), "key-1"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_customerNotFound_throwsResourceNotFoundExceptionAndNeverSaves() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenThrow(notFound());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> orderService.create(newRequest(), "key-1"));

        verify(productClient, never()).getProduct(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_productServiceUnavailable_throwsBusinessRuleExceptionAndNeverSaves() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));
        when(productClient.getProduct(1L)).thenThrow(serviceUnavailable());

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> orderService.create(newRequest(), "key-1"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_customerServiceUnavailable_throwsBusinessRuleExceptionAndNeverSaves() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenThrow(serviceUnavailable());

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> orderService.create(newRequest(), "key-1"));

        verify(productClient, never()).getProduct(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_concurrentDuplicateIdempotencyKey_returnsWinningOrderInsteadOfSurfacingConstraintViolation() {
        Order winningOrder = Order.builder().id(7L).customerId(1L).productId(1L).quantity(2).total(79.80).build();
        IdempotencyKey winningKey = IdempotencyKey.builder().idempotencyKey("key-1").order(winningOrder).build();
        // First read (the pre-check): nothing yet. Second read (after the losing insert's
        // constraint violation): the concurrent winner's row is now visible.
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningKey));
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));
        when(productClient.getProduct(1L)).thenReturn(ApiResponse.success(product(39.90), "ok"));
        when(orderRepository.save(any(Order.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        OrderResponse response = orderService.create(newRequest(), "key-1");

        assertThat(response.id()).isEqualTo(7L);
    }

    @Test
    void create_publishesOrderCreatedEventOnlyAfterCommit_neverBefore() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));
        when(productClient.getProduct(1L)).thenReturn(ApiResponse.success(product(39.90), "ok"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });

        TransactionSynchronizationManager.initSynchronization();
        orderService.create(newRequest(), "key-1");

        verify(orderEventProducer, never()).publishOrderCreated(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        verify(orderEventProducer).publishOrderCreated(any());
    }

    @Test
    void create_neverPublishesOrderCreatedEventOnRollback() {
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));
        when(productClient.getProduct(1L)).thenReturn(ApiResponse.success(product(39.90), "ok"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });

        TransactionSynchronizationManager.initSynchronization();
        orderService.create(newRequest(), "key-1");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    void findAll_delegatesToRepository() {
        Order order = Order.builder().id(1L).customerId(1L).productId(1L).quantity(1).total(39.90).build();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(order), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(page);

        assertThat(orderService.findAll(pageable).getContent()).hasSize(1);
    }

    @Test
    void findById_found_returnsEnrichedDetailResponse() {
        Order order = Order.builder().id(1L).customerId(1L).productId(1L).quantity(2).total(79.80).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productClient.getProduct(1L)).thenReturn(ApiResponse.success(product(39.90), "ok"));
        when(customerClient.getCustomer(1L)).thenReturn(ApiResponse.success(customer(), "ok"));

        OrderDetailResponse response = orderService.findById(1L);

        assertThat(response.productName()).isEqualTo("Clean Code");
        assertThat(response.customerFullName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void findById_orderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> orderService.findById(99L));
    }

    @Test
    void markConfirmed_transitionsPendingToConfirmed() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markConfirmed(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void markConfirmationFailed_transitionsPendingToConfirmationFailed() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markConfirmationFailed(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMATION_FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    void markConfirmed_redeliveredEventOnAlreadyConfirmedOrder_isNoOp() {
        Order order = Order.builder().id(1L).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markConfirmed(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void markConfirmationFailed_redeliveredEventOnAlreadyConfirmationFailedOrder_isNoOp() {
        Order order = Order.builder().id(1L).status(OrderStatus.CONFIRMATION_FAILED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markConfirmationFailed(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMATION_FAILED);
        verify(orderRepository, never()).save(any());
    }
}
