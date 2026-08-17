# Multi-stage build for discovery-server, a module of the spring-microservices-tutorial
# Maven reactor (monorepo). Build context is the repository root (see docker-compose.yml,
# "context: ." / "dockerfile: services/discovery-server/Dockerfile"), because Maven needs
# the aggregator pom.xml and this module's own dependency, common-lib, to build.
#
# Design choice (monorepo Maven multi-module build in Docker):
# We copy every module's pom.xml first (the reactor aggregator pom.xml lists all modules,
# so Maven needs each one's pom.xml present to resolve the reactor graph), then copy only
# the source of the modules this image actually needs: common-lib (shared dependency) and
# discovery-server itself. `mvn -pl services/discovery-server -am package` then builds
# discovery-server and "also make"s (-am) its own module dependencies (common-lib), without
# compiling every other service's source in this image. This keeps the image build scoped
# to what discovery-server needs while still resolving through the real reactor, and keeps
# the pom-only layer cacheable independently of source changes elsewhere in the monorepo.
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
COPY services/discovery-server/src services/discovery-server/src

RUN mvn -B -pl services/discovery-server -am package -DskipTests

# Minimal JRE runtime, no Maven/JDK left in the final image.
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/services/discovery-server/target/discovery-server-*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
