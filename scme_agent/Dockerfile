FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY target/ScmeTrapSender.jar /app/ScmeTrapSender.jar

COPY trapsend.dat /app/data/trapsend.dat


CMD ["java", "-jar", "/app/ScmeTrapSender.jar"]
