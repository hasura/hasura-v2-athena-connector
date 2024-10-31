plugins {
    `kotlin-library-conventions`
    kotlin("plugin.allopen") version "1.8.21"

    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

object ProjectVersions {
    const val strikt = "0.34.1"
    const val athena = "42-2.0.36.1000"
    const val documentdb = "1.3.0"
    const val snowflake = "3.13.29"
    const val trino = "403"
    const val redshift = "2.1.0.28"
    const val quarkus_logging_manager = "2.1.4"
}

repositories {
    mavenCentral()
    mavenLocal()

}

dependencies {
    implementation(project(":gdc-ir"))
    implementation(project(":sql-gen"))
    implementation(kotlin("script-runtime"))

    // NOTE: Changed "enforcedPlatform" to "platform" to allow overriding of versions in the Quarkus BOM
    // for vulnerabilities reported by Aquasec Trivy. This may have unintended consequences.
    implementation(platform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${GlobalVersions.jackson}")
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-vertx")
    implementation("io.quarkus:quarkus-reactive-routes")
    implementation("io.quarkus:quarkus-logging-json")

    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.opentelemetry:opentelemetry-extension-kotlin")
    implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
    implementation("io.opentelemetry.instrumentation:opentelemetry-jdbc")

    // Include any jars in the "../lib/vendored" directory
    implementation(
        fileTree(
            mapOf(
                "dir" to "../lib/vendored",
                "include" to listOf("*.jar")
            )
        )
    )

    // JDBC Drivers
    implementation(files("../lib/AthenaJDBC${ProjectVersions.athena}.jar"))
    implementation("net.snowflake:snowflake-jdbc:${ProjectVersions.snowflake}") // Used in PrivateKeyHandler.kt

    implementation("com.google.protobuf:protobuf-java:3.25.5")

    // Spring JDBC ScriptUtils used for loading .sql files
    implementation("org.springframework:spring-jdbc:6.0.16")

    // AWS alternate auth mechanisms
    implementation("software.amazon.awssdk:sts:2.23.15")
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:2.0.0")
    implementation("software.amazon.jdbc:aws-advanced-jdbc-wrapper:2.3.2")
    implementation("software.amazon.awssdk:rds:2.23.8")

    // JOOQ
    implementation("${GlobalVersions.jooqEdition}:jooq:${GlobalVersions.jooq}")

    // SchemaCrawler
    implementation("us.fatehi:schemacrawler:${GlobalVersions.schemacrawler}")
    listOf("trino").forEach {
        implementation("us.fatehi:schemacrawler-$it:${GlobalVersions.schemacrawler}")
    }

    implementation("io.agroal:agroal-api:2.3")
    implementation("io.agroal:agroal-pool:2.3")
    implementation("io.agroal:agroal-narayana:2.3")

    // JWT
    implementation("org.bitbucket.b_c:jose4j:${GlobalVersions.jose4j}")


    // //////////////////////
    // Test Dependencies
    // //////////////////////
    testImplementation("io.quarkus:quarkus-junit5")

    // RestAssured
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.quarkusDev.configure {
    jvmArgs = Constants.JVM_EXEC_ARGS + listOf(
        // The below is needed to make the Quarkus application reachable
        // from Docker containers when developing inside of WSL2 and run as a standard JVM process
        // (IE so Hasura can reach it)
        "-Djava.net.preferIPv4Stack=true",
        "-Djava.net.preferIPv4Addresses=true",
    )
    compilerOptions {
        compiler("kotlin").args(Constants.KOTLIN_COMPILER_ARGS)
    }
}
