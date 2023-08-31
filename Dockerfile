FROM openjdk:17-ea-slim-buster

COPY target/enexa-service-0.0.1-SNAPSHOT.jar /app/enexa-service-0.0.1-SNAPSHOT.jar

CMD java -jar /app/enexa-service-0.0.1-SNAPSHOT.jar
