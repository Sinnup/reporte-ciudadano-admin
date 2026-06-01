plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "com.espert.reporteciudadanoadmin"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.espert.reporteciudadanoadmin.ApplicationKt")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("backend-all.jar")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.aws.dynamodb)
    implementation(libs.aws.s3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
}
