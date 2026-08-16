# Multi-stage build for notification-service, a module of the spring-microservices-tutorial
# Maven reactor (monorepo). Build context is the repository root (see docker-compose.yml,
# "context: ." / "dockerfile: services/notification-service/Dockerfile"), because Maven needs
# the aggregator pom.xml and this module's own dependency, common-lib, to build.
#
# Same monorepo build approach as every other service's Dockerfile: every module's pom.xml is
# copied first so Maven's reactor can resolve the full module graph declared in the root
# pom.xml, then only the source this image actually needs (common-lib and
# notification-service) is copied in. `mvn -pl services/notification-service -am package`
# builds notification-service plus its module dependency (common-lib, "also make") without
# compiling every other service.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY common-lib/pom.xml common-lib/pom.xml
COPY services/discovery-server/pom.xml services/discovery-server/pom.xml
COPY services/config-server/pom.xml services/config-server/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/catalog-service/pom.xml services/catalog-service/pom.xml
COPY services/customer-service/pom.xml services/customer-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

COPY common-lib/src common-lib/src
COPY services/notification-service/src services/notification-service/src

RUN mvn -B -pl services/notification-service -am package -DskipTests

# Minimal JRE runtime, no Maven/JDK left in the final image.
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/services/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
