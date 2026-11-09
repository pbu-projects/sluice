plugins {
    id("groovy")
    id("io.micronaut.application") version "5.0.2"
    id("com.gradleup.shadow") version "9.4.1"
    id("io.micronaut.aot") version "5.0.2"
    id("jacoco")
}

version = project.properties["sluiceVersion"]!!
group = "lol.pbu.sluice"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")

    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.locationtech.jts:jts-core:${project.properties["jtsVersion"]}")
    implementation("org.locationtech.jts.io:jts-io-common:${project.properties["jtsVersion"]}")
    implementation("io.micronaut:micronaut-management")

    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.yaml:snakeyaml")

    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.apache.groovy:groovy-json")
}

application {
    mainClass = "lol.pbu.sluice.Application"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

graalvmNative.toolchainDetection = false
graalvmNative {
    binaries {
        all {
            buildArgs.add("-H:+SharedArenaSupport")
        }
    }
}

micronaut {
    runtime("netty")
    testRuntime("spock2")
    processing {
        incremental(true)
        annotations("lol.pbu.sluice.*")
    }
    aot {
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}

tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
