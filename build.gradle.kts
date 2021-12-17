import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.6.10"
    id("tech.formatter-kt.formatter") version "0.7.9"
    application
}

group = "electionguard-kotlin-benchmark"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Kinda unclear what exactly goes here
    //    implementation(kotlin("stdlib-jvm", "1.6.10"))
    //    implementation(kotlin("stdlib-common", "1.6.10"))
    implementation(kotlin("stdlib", "1.6.10"))

    // JSON serialization and DSL
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")

    // Alternative math library
    implementation("io.github.gciatto:kt-math:0.4.0")

    // logging
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.16")

    // tqdm-ish progressbar
    implementation("me.tongfei:progressbar:0.9.2")

    // Property-based testing
    testImplementation("io.kotest:kotest-property:5.0.1")

    // JUnit5 support
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.test {
    useJUnitPlatform()
    minHeapSize = "512m"
    maxHeapSize = "2048m"
    jvmArgs = listOf("-Xss128m")
    systemProperty("junit.jupiter.execution.parallel.enabled", true)
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        //        languageSettings.optIn("kotlin.RequiresOptIn")
        freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

application {
    mainClass.set("electionguard.ElGamalBenchmarkKt")
    applicationDefaultJvmArgs = listOf("-Xss128m", "-Xms512m", "-Xmx2048m")
    //    minHeapSize = "512m"
    //    maxHeapSize = "2048m"
    //    jvmArgs = listOf("-Xss128m")
}

tasks.withType<Test> { testLogging { showStandardStreams = true } }