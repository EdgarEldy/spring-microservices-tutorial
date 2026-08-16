package com.edgareldy.springmicroservicestutorial.customerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapping the {@code customers} table in {@code customer_db}: a
 * business-facing profile (name, phone, e-mail, address) for someone who has
 * already registered as a {@code User} in {@code auth-service}.
 * <p>
 * {@link #userId} is deliberately a plain {@code Long} column
 * ({@code @Column(name = "user_id")}), never a JPA relationship
 * ({@code @ManyToOne}/{@code @OneToOne}) and never backed by a foreign key
 * in {@code V1__init_schema.sql}: {@code customer_db} and {@code auth_db}
 * are two separate databases owned by two separate services, and a
 * cross-service foreign key (or a JPA association that implies one) would
 * mean this service could no longer be deployed, migrated, or scaled
 * independently of {@code auth-service}. The only supported way to resolve
 * what a {@code userId} actually is (name, e-mail, roles, ...) is
 * {@code auth-service}'s own API, never a join or a direct query against
 * {@code auth_db}. See README's "Why users and customers stay in separate
 * services" and the "no cross-service foreign key" rule.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain value referencing auth-service's users.id. Never a FK, never a
    // JPA association: see the class Javadoc above.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "telephone", nullable = false)
    private String telephone;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "address", nullable = false)
    private String address;
}
