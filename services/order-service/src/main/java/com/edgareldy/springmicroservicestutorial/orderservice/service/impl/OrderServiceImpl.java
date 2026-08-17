package com.edgareldy.springmicroservicestutorial.orderservice.service.impl;

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
import com.edgareldy.springmicroservicestutorial.orderservice.event.OrderCreatedEvent;
import com.edgareldy.springmicroservicestutorial.orderservice.event.OrderEventProducer;
import com.edgareldy.springmicroservicestutorial.orderservice.mapper.OrderMapper;
import com.edgareldy.springmicroservicestutorial.orderservice.repository.IdempotencyKeyRepository;
import com.edgareldy.springmicroservicestutorial.orderservice.repository.OrderRepository;
import com.edgareldy.springmicroservicestutorial.orderservice.service.OrderService;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Default {@link OrderService} implementation: the only service in this project that calls
 * others synchronously (via {@link ProductClient}/{@link CustomerClient}) and the origin of
 * its asynchronous flow ({@link OrderEventProducer}).
 * <p>
 * <strong>Publish-after-commit design decision</strong>: {@link #create} persists the
 * {@code Order}/{@code IdempotencyKey} and must publish {@link OrderCreatedEvent}, but never
 * before or during that transaction. Same approach as {@code auth-service}'s
 * {@code AuthServiceImpl}: {@code TransactionSynchronizationManager.registerSynchronization(...)},
 * registered from inside the still-active {@code @Transactional} method, with an
 * {@code afterCommit()} callback into {@link OrderEventProducer}, rather than extracting
 * persistence into a separate {@code private @Transactional} method (Spring's self-invocation
 * pitfall would silently skip that method's own transaction demarcation).
 * <p>
 * <strong>Concurrent duplicate requests</strong>: the idempotency pre-check in {@link #create}
 * and the actual insert happen as two separate steps, so two requests carrying the same
 * {@code Idempotency-Key} arriving at the same time can both pass the pre-check before either
 * commits. The loser's insert then fails on {@code idempotency_keys}' {@code UNIQUE} constraint,
 * throwing {@link DataIntegrityViolationException}, which rolls back that entire attempt
 * (including its own {@code Order} row, so no duplicate order survives). {@link #create}
 * catches that specific exception and re-reads the now-committed winning row instead of
 * letting a raw constraint violation surface as a 500, delegating the insert itself to
 * {@link #createAndPersist}, a separate {@code @Transactional} public method invoked through
 * {@link #self} (a {@code @Lazy}-injected reference to this same bean's Spring proxy): calling
 * it directly via {@code this.createAndPersist(...)} would bypass the proxy entirely (Spring's
 * self-invocation pitfall, see {@code auth-service}'s {@code AuthServiceImpl} for the same
 * pitfall in a different shape), silently skipping its {@code @Transactional} boundary and
 * defeating the whole point of isolating the insert into its own rollback-able unit.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final CustomerClient customerClient;
    private final OrderEventProducer orderEventProducer;

    @Lazy
    private final OrderServiceImpl self;

    @Override
    public OrderResponse create(OrderRequest request, String idempotencyKey) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return orderMapper.toResponse(existing.get().getOrder());
        }

        try {
            return self.createAndPersist(request, idempotencyKey);
        } catch (DataIntegrityViolationException ex) {
            return idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                    .map(key -> orderMapper.toResponse(key.getOrder()))
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Validates via both Feign clients, computes {@code total}, and persists the order and its
     * idempotency key in one transaction. Package-visible only through the public interface
     * because {@code @Transactional} needs it invoked via the Spring proxy, never called
     * directly from {@link #create} within this same instance (see class Javadoc).
     */
    @Transactional
    public OrderResponse createAndPersist(OrderRequest request, String idempotencyKey) {
        resolveCustomer(request.customerId());
        ProductResponse product = resolveProduct(request.productId());

        Order order = orderMapper.toEntity(request);
        order.setTotal(request.quantity() * product.unitPrice());
        Order savedOrder = orderRepository.save(order);

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .order(savedOrder)
                .createdAt(Instant.now())
                .build());

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getProductId(),
                savedOrder.getQuantity(), savedOrder.getTotal());
        publishAfterCommit(() -> orderEventProducer.publishOrderCreated(event));

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No order found with id " + id));
        ProductResponse product = resolveProduct(order.getProductId());
        CustomerResponse customer = resolveCustomer(order.getCustomerId());

        return new OrderDetailResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotal(),
                order.getStatus(),
                product.productName(),
                customer.firstName() + " " + customer.lastName());
    }

    @Override
    @Transactional
    public void markConfirmed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No order found with id " + orderId));
        // Kafka delivers at least once: a redelivered OrderConfirmedEvent for an order
        // already CONFIRMED (or already CONFIRMATION_FAILED, a state this event should
        // never legitimately follow) is a no-op, not a re-application, matching the
        // Idempotent Consumer pattern this branch is built around.
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void markConfirmationFailed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No order found with id " + orderId));
        // Same idempotent-redelivery reasoning as markConfirmed.
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMATION_FAILED);
        orderRepository.save(order);
    }

    // No try/catch here (see feature/resilience): ProductClientFallbackFactory/
    // CustomerClientFallbackFactory now own the FeignException -> ResourceNotFoundException/
    // BusinessRuleException translation, invoked by the Resilience4j circuit breaker
    // wrapping every call (spring.cloud.openfeign.circuitbreaker.enabled=true). A plain FeignException never
    // reaches this class any more, whether the circuit is open or the call itself failed.
    private ProductResponse resolveProduct(Long productId) {
        return productClient.getProduct(productId).data();
    }

    private CustomerResponse resolveCustomer(Long customerId) {
        return customerClient.getCustomer(customerId).data();
    }

    private void publishAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
