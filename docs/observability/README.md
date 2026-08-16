# feature/observability: distributed tracing walkthrough

Distributed tracing across the whole system, using Micrometer Tracing (Brave bridge) and
Zipkin. This is the "short walkthrough... showing a captured trace in the Zipkin UI for a
full order-creation request, annotated with what each span represents" the root `README.md`'s
`feature/observability` task list asks for: what was added, two real bugs this branch's own
live verification surfaced and fixed, how to reproduce it, and what each span in the two
traces represents.

## What changed

- `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` added to every service's
  `pom.xml` (discovery-server, config-server, and all 6 business services). Both versions
  come from `spring-boot-dependencies`' own `micrometer-tracing-bom`/`zipkin-reporter-bom`
  imports, no explicit `<version>` needed.
- **`spring-boot-micrometer-tracing-brave` also added to every service, and it is not
  optional.** Spring Boot 4.1 moved `BraveAutoConfiguration`/
  `ZipkinWithBraveTracingAutoConfiguration` (the glue that actually creates the `Tracer`
  bean, wires the MDC `traceId`/`spanId` scope decorator, and configures the Zipkin
  reporter) out of `spring-boot-actuator-autoconfigure` into this dedicated module, the same
  class of module-splitting already seen with `spring-boot-kafka` elsewhere in this project.
  Without it, `micrometer-tracing-bridge-brave`/`zipkin-reporter-brave` sit on the classpath
  and do nothing: no `Tracer` bean, no MDC correlation, no spans ever reported, and no error
  or warning anywhere to say so. Found by actually running the stack and checking
  `/actuator/conditions`: zero tracing-related autoconfiguration classes were even being
  evaluated. Fixed, then reverified live (see [LoggingAspect re-verified](#loggingaspect-re-verified)).
- `management.tracing.sampling.probability: 1.0` (100% sampling, a teaching-project choice,
  would be lowered in real production) and `management.zipkin.tracing.endpoint`
  (`${ZIPKIN_URI:http://localhost:9411/api/v2/spans}`, same env-var-overridable pattern as
  `EUREKA_URI`) added to every service's config.
- `spring.kafka.template.observation-enabled` / `spring.kafka.listener.observation-enabled`
  explicitly set to `true` on `auth-service` (producer only), `order-service`, and
  `notification-service` (both). Not automatic just from having the tracing bridge on the
  classpath: both properties default to `false` in `spring-boot-kafka`, and without them a
  Kafka producer/consumer never injects or extracts trace context into/from record headers,
  so the "Kafka spans connect into one trace" half of this feature would silently not happen.
  `catalog-service`/`customer-service` never touch Kafka, no change needed there.
- A `zipkin` service (`openzipkin/zipkin:latest`) added to `docker-compose.yml`, port `9411`
  published to the host for local UI access (same "convenience infra" treatment as
  postgres/kafka/redis, see that file's own header comment).
- **Two unrelated real bugs found and fixed along the way**, while actually running the full
  stack for this branch's own live verification (neither one this branch's fault, both
  pre-existing, both previously undetected because no test in this project ever loaded a
  full, real application context that exercised them):
  - `lombok.config` added at the repo root (`lombok.copyableAnnotations +=
    org.springframework.context.annotation.Lazy`). Without it, Lombok's
    `@RequiredArgsConstructor` silently drops `@Lazy` when generating a constructor
    parameter from an annotated field. `order-service`'s `OrderServiceImpl` relies on `@Lazy
    OrderServiceImpl self` (self-injection through its own Spring proxy) to avoid an eager
    circular-dependency failure at startup; without this fix, `order-service` fails to start
    at all ("dependencies of some of the beans... form a cycle"), verified live and fixed by
    inspecting the generated constructor's bytecode for the `@Lazy` parameter annotation.
  - `@Import(LoggingAspect.class)` added to every service's `*Application.java`. Bare
    `@SpringBootApplication` only scans its own package and sub-packages; `common-lib`'s
    classes live in a sibling package (`...commonlib`, not a sub-package of e.g.
    `...orderservice`), so `LoggingAspect` (a plain `@Component @Aspect`, meant to be
    auto-detected) was **never registered as a bean in any service, in this entire project,
    since it was written** - its `@Around` advice never ran once, anywhere, silently. This is
    exactly what this branch's own "LoggingAspect re-verified" task item was supposed to
    check by hand; the check found the aspect completely inert rather than merely missing
    trace IDs. The first fix attempted here was a wider
    `@ComponentScan(basePackages = "com.edgareldy.springmicroservicestutorial")` on
    `OrderServiceApplication`: it did fix `LoggingAspect`, but it also broke
    `OrderControllerTest` (a `@WebMvcTest` slice) - its usual `TypeExcludeFilter` stopped
    reliably keeping `JwtService`'s real bean out of that slice context, which meant its
    `@MockitoBean` override no longer took effect; the real `JwtService` constructor ran
    against the test's short JWT secret and threw `WeakKeyException`. Reproduced by reverting
    just that one annotation and re-running the test (passed), then reapplying it (failed
    again), confirming the cause before switching to the single-class `@Import` used in the
    final code: it registers exactly one bean, with no interaction with component-scan-based
    slice-test filtering at all. Full reactor suite (221 tests) passes with `@Import`.

## Two real bugs, found by actually running the stack

Both of the bugs above were invisible to `mvn test` across the whole reactor (221 tests, 0
failures, both times, before and after these fixes) because no existing test in this project
loads a full, real `*Application` context for `order-service` and then exercises a service
method through the real AOP proxy chain with debug logging enabled. Every existing test
either mocks the collaborator directly, uses a minimal hand-picked context, or never turns on
debug logging for `common-lib`'s package specifically. This is the concrete value of the
"verify by hand once, not assumed" instruction on this branch's task list: a passing test
suite and a service that actually starts and does the right thing under real conditions are
not the same claim.

## How to verify: reproducing the two traces

Bring up the full stack:

```bash
docker compose up -d --build
```

Wait for every service to report healthy (`docker compose ps`), then:

### 1. Register, activate, and log in a user

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","password":"Sup3rSecret!"}'
```

`notification-service` logs the activation e-mail it "sent" (see the README's "single point of
exit" rule, this tutorial only ever logs e-mails). Grab the token from `docker compose logs
notification-service`, then:

```bash
curl -s "http://localhost:8080/api/v1/auth/activate-account?token=<token-from-the-log>"

curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@example.com","password":"Sup3rSecret!"}'
```

Keep the `token` field from the login response; call it `$JWT` below.

### 2. Grant ADMIN (no seed data exists in any migration)

`catalog-service`'s `POST /api/v1/catalog/categories`/`products` require `hasRole('ADMIN')`,
and no `V1__init_schema.sql` in this project seeds a default role. One-time bootstrap, direct
against the database (never how a real deployment would grant a role, an admin-management
endpoint is out of this tutorial's scope):

```bash
docker compose exec postgres psql -U postgres -d auth_db -c "
  INSERT INTO roles (role_name) VALUES ('ADMIN')
    ON CONFLICT DO NOTHING;
  INSERT INTO role_user (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'ada@example.com' AND r.role_name = 'ADMIN';
"
```

Log in again (JWTs carry the roles claim as of *issue* time, the first token from step 1 does
not retroactively gain the role) and use this second `$JWT` from here on.

### 3. Create a category, a product, and a customer

```bash
CATEGORY_ID=$(curl -s -X POST http://localhost:8080/api/v1/catalog/categories \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d '{"categoryName":"Books"}' | jq -r '.data.id')

PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/v1/catalog/products \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d "{\"productName\":\"Structure and Interpretation of Computer Programs\",\"unitPrice\":39.90,\"categoryId\":$CATEGORY_ID}" \
  | jq -r '.data.id')

CUSTOMER_ID=$(curl -s -X POST http://localhost:8080/api/v1/customers \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d '{"userId":1,"firstName":"Ada","lastName":"Lovelace","telephone":"+10000000000","email":"ada@example.com","address":"1 Analytical Engine Way"}' \
  | jq -r '.data.id')
```

### 4. Create the order: the request that generates both traces

```bash
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d "{\"customerId\":$CUSTOMER_ID,\"productId\":$PRODUCT_ID,\"quantity\":1}"
```

This request, and the whole choreographed Saga it triggers, was verified live end to end on
this branch: the order genuinely goes `PENDING` -> `CONFIRMED`, resolved via
`GET /api/v1/orders/{id}` afterwards, confirming both the synchronous Feign half (product/
customer resolved) and the asynchronous Kafka half (order-service -> notification-service ->
back to order-service) actually ran.

### 5. Pull the traces from Zipkin

```bash
curl -s "http://localhost:9411/api/v2/traces?serviceName=api-gateway&limit=5" | jq .
```

Zipkin's `/api/v2/traces` returns an array of traces, each an array of spans sharing one
`traceId`; every span carries its own `id`, `parentId`, `name`, `localEndpoint.serviceName`,
`timestamp`, and `duration` (microseconds). The UI at `http://localhost:9411` renders the
same data as a waterfall/flame graph, generally the easier way to read it.

## Annotated trace #1: the Feign hop

Triggered by the `POST /api/v1/orders` call above (and equally by a later
`GET /api/v1/orders/{id}`, which resolves the same two clients for its API Composition
response). One connected trace, `api-gateway` as the root:

| Span (`localEndpoint.serviceName`) | What it represents |
|---|---|
| `api-gateway` - HTTP server span for the inbound request | `JwtValidationGatewayFilter` validates the JWT, then `RouteConfig`'s `order-service` route proxies onward via `lb://order-service` |
| `order-service` - HTTP server span for `POST /api/v1/orders` | `OrderController` receiving the proxied request; this is where `OrderServiceImpl.createAndPersist` runs |
| `order-service` - Feign client span to `catalog-service` | `ProductClient.getProduct(productId)`, called from `OrderServiceImpl.resolveProduct` before the order is persisted |
| `catalog-service` - HTTP server span for `GET /api/v1/catalog/products/{id}` | `ProductController` handling that Feign call, its own local span, connected as a child of the Feign client span above via the propagated `traceId` |
| `order-service` - Feign client span to `customer-service` | `CustomerClient.getCustomer(customerId)`, `OrderServiceImpl.resolveCustomer` |
| `customer-service` - HTTP server span for `GET /api/v1/customers/{id}` | `CustomerController` handling that call |

Micrometer Tracing instruments Spring MVC (server spans), Spring Cloud Gateway (the routing
span), and OpenFeign (client spans) automatically once both tracing dependencies (see
[What changed](#what-changed)) are present, no manual span creation anywhere in this
project's own code.

## Annotated trace #2: the Kafka hop

Triggered by the same `POST /api/v1/orders` call, *after* the local transaction commits (see
the README's "publish after commit" rule): `OrderServiceImpl.publishAfterCommit` calls
`OrderEventProducer.publishOrderCreated`, which is where the Kafka half of the trace begins.

Because `spring.kafka.template.observation-enabled`/`listener.observation-enabled` are both
`true` (see [What changed](#what-changed) above), Spring for Apache Kafka's built-in
observation instrumentation injects the current trace context into the Kafka record's headers
on send, and extracts it back out on receive - the consumer side genuinely continues the
*same* trace as the producer, it does not start a new one:

| Span (`localEndpoint.serviceName`) | What it represents |
|---|---|
| `order-service` - Kafka producer span, `order-events send` | `OrderEventProducer.publishOrderCreated`, publishing `OrderCreatedEvent` |
| `notification-service` - Kafka consumer span, `order-events receive` | `OrderCreatedEventConsumer.onOrderCreated`, the `@KafkaListener` on `order-events` |

`notification-service` then publishes `OrderConfirmedEvent` (or `NotificationFailedEvent`, if
the simulated-failure product ID was used) back onto `notification-events`, and
`order-service`'s own `OrderConfirmedEventListener`/`NotificationFailedEventListener` consumes
that in turn - both still under the same propagated `traceId`. A single request through the
gateway can therefore surface as **one connected trace covering the entire choreographed
Saga**: gateway to order-service, order-service to catalog/customer-service (the synchronous
half), order-service to notification-service and back again (the asynchronous half). The
README frames the Feign hop and the Kafka hop as two separate checkpoints to verify; expect
them to actually land as segments of that one trace rather than two disconnected ones, which
is the more complete demonstration of the choreography, and matches what the live run on this
branch actually did (order status genuinely reached `CONFIRMED` via the full round trip).

## `LoggingAspect` re-verified

`common-lib/src/main/java/.../commonlib/logging/LoggingAspect.java` calls only
`log.debug(...)`/`log.info(...)`, with no MDC-related code at all, before and after this
branch - it never needed a code change. What it needed was to actually be registered as a
Spring bean at all (see [What changed](#what-changed): the `@Import(LoggingAspect.class)`
fix) and a real `Tracer` bean to exist so Micrometer Tracing's Brave bridge populates the MDC
(`spring-boot-micrometer-tracing-brave` fix). Verified live, with both fixes applied, calling
`order-service` directly and grepping its own log output:

```
2026-08-16T22:59:39.751+04:00 DEBUG 13093 --- [order-service] [nio-8084-exec-2] [6a82089b21bdf806871aa11751222bae-ef1a82bef7076f26] c.e.s.commonlib.logging.LoggingAspect    : Entering OrderServiceImpl.findById(..) with arguments [1]
```

The bracketed `[6a82089b21bdf806871aa11751222bae-ef1a82bef7076f26]` segment (Spring Boot's
own log correlation pattern, `%X{traceId:-}-%X{spanId:-}`) is a genuine `traceId`/`spanId`
pair pulled from the SLF4J MDC during the request, not a placeholder: before the two fixes in
this branch, that same call produced no `LoggingAspect` output at all (the aspect was never a
bean), and immediately after fixing only the missing-bean gap (before also adding
`spring-boot-micrometer-tracing-brave`), the aspect's log lines appeared but with no
correlation bracket at all, confirming the second fix was independently necessary too.
