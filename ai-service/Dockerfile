FROM eclipse-temurin:17-jdk-jammy

VOLUME /tmp
EXPOSE 8085
RUN mkdir -p /app/
RUN mkdir -p /app/logs/

# copy the jar built by: mvn clean package -Dmaven.test.skip=true
ADD target/ai-service-0.0.1-SNAPSHOT.jar /app/app.jar

WORKDIR /app
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=dev", "-jar", "/app/app.jar"]