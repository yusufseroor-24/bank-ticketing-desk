FROM eclipse-temurin:17-jdk-jammy

VOLUME /tmp
EXPOSE 8085
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app
RUN mkdir -p /app/
RUN mkdir -p /app/logs/
RUN mkdir -p /app/logs /app/uploads && chown -R app:app /app

# copy the jar built by: mvn clean package -Dmaven.test.skip=true
ADD target/ai-service-0.0.1-SNAPSHOT.jar /app/app.jar

WORKDIR /app
USER app
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=dev", "-jar", "/app/app.jar"]