plugins {
    java
    application
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.team"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    implementation(platform("org.eclipse.jetty:jetty-bom:12.1.8"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.2"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.javalin:javalin:7.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.postgresql:postgresql:42.7.10")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.zaxxer:HikariCP:7.0.2")

    implementation("org.flywaydb:flyway-core:12.4.0")
    implementation("org.flywaydb:flyway-database-postgresql:12.4.0")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.assertj:assertj-core:3.27.7")

    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")


    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))

    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")

    testImplementation("io.rest-assured:rest-assured:5.5.2")
    testImplementation("org.apache.commons:commons-lang3:3.20.0")
    testImplementation("commons-codec:commons-codec:1.21.0")

}

application {
    mainClass.set("com.team.lottery.Application")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}

tasks.build {
    dependsOn("shadowJar")
}