import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "org.example.project"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("org.example.project.ApplicationKt")
    val isDev: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDev")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    // Seguridad / observabilidad
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation("com.github.librepdf:openpdf:1.3.39")
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation("com.h2database:h2:2.2.224")
}

// Configure ShadowJar
tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("server")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest { attributes(mapOf("Main-Class" to application.mainClass.get())) }
    doLast {
        val jar = archiveFile.get().asFile
        val fixed = jar.resolveSibling("server.jar")
        if (fixed.exists()) fixed.delete()
        jar.copyTo(fixed, overwrite = true)
        println("Generado también: ${fixed.name}")
    }
}

// Alias to build fat-JAR
tasks.register("fatJar") {
    group = "build"
    description = "Build fat-jar using Shadow"
    dependsOn("shadowJar")
}
