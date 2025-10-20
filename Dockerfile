# Build stage
FROM maven:3.8.6-openjdk-11-slim AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim
WORKDIR /app

# Copy the compiled jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the PDF catalog
COPY menu-quitutes.pdf .

# Run the bot
ENTRYPOINT ["java", "-jar", "app.jar"]

