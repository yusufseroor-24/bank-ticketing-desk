FROM eclipse-temurin:17-jdk-jammy

# create temporary drive
VOLUME /tmp

# expose the port of this service
EXPOSE 8084

# create app dir
RUN mkdir -p /app/

# create logs dir
RUN mkdir -p /app/logs/

# copy the jar built by: mvn clean package -Dmaven.test.skip=true
ADD target/notification-service-0.0.1-SNAPSHOT.jar /app/app.jar

# uploads are written relative to /app
WORKDIR /app

# run the .jar file with the active profile dev
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=dev", "-jar", "/app/app.jar"]