# Multi-stage container build for Sluice using Java 25 and Micronaut AOT

FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./

RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew optimizedJitJarAll --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /home/app

RUN groupadd -g 10001 sluice && \
    useradd -u 10001 -g sluice -m -s /bin/false sluice

COPY --from=build --chown=sluice:sluice /home/app/build/libs/sluice-*-all-optimized.jar /home/app/application.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/home/app/application.jar"]
