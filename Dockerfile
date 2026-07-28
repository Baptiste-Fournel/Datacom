FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY config config
COPY src src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S datacom && adduser -S datacom -G datacom
USER datacom
COPY --from=build /build/target/datacom-*.jar /app/datacom.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/datacom.jar"]
