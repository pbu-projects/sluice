# Multi-stage build for Sluice using GraalVM 25 Native Image

FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /home/app

COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./

RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew nativeOptimizedCompile --no-daemon

# Minimal Debian runtime compatible with the build image glibc
FROM debian:trixie-slim
WORKDIR /home/app

RUN groupadd -g 10001 sluice && \
    useradd -u 10001 -g sluice -m -s /bin/false sluice

COPY --from=build --chown=sluice:sluice /home/app/build/native/nativeOptimizedCompile/sluice /home/app/sluice

USER 10001:10001

EXPOSE 8085

ENTRYPOINT ["/home/app/sluice"]
