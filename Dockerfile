
FROM maven:3.6.1-jdk-11 AS MAVEN_BUILD

COPY ./ ./
 
RUN mvn clean package
 
FROM openjdk:11-jre-slim

COPY --from=MAVEN_BUILD target/service-bus-0.0.1-SNAPSHOT.jar /service-bus.jar

CMD ["java", "-jar", "/service-bus.jar"]
