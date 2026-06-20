plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.example.padlecano.backend.ApplicationKt")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.auth0.jwt)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback.classic)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
