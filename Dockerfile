# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /opt/app

# Copy parent pom
COPY pom.xml .

# Copy module poms only
COPY chat-common/pom.xml chat-common/pom.xml
COPY chat-server/pom.xml chat-server/pom.xml
# Copying chat client as well for dependency resolution
COPY chat-client/pom.xml chat-client/pom.xml

# Resolve dependencies
RUN mvn dependency:go-offline -pl chat-server -am

# Copy source
COPY chat-common/src chat-common/src
COPY chat-server/src chat-server/src

# Build only server and required modules
RUN mvn package -pl chat-server -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre AS final

WORKDIR /opt/app

# Run application as a non-root user
RUN useradd --system --create-home appuser \
    && mkdir -p /opt/app/images \
    && chown -R appuser:appuser /opt/app

COPY --from=build /opt/app/chat-server/target/*.jar app.jar

USER appuser

EXPOSE 8443
EXPOSE 5000

ENTRYPOINT ["java", "-jar", "app.jar"]