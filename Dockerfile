FROM maven:3-openjdk-18 as builder
WORKDIR /app-build
COPY . .
RUN mvn package



FROM openjdk:17-jdk-alpine
WORKDIR /app/
COPY --from=builder /app-build/target/demo-0.0.1-SNAPSHOT.jar /app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/demo-0.0.1-SNAPSHOT.jar"]
