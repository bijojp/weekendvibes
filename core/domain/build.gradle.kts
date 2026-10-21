// Domain module: Activities, Extra days, the rules that turn free days into Breaks, and reading
// event pages for Link import. Pure Kotlin with no Android dependencies, so every rule is
// unit-tested on the JVM, including against saved copies of real event pages.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    // Event pages: HTML parsing, and the JSON-LD inside them (no serialization plugin needed).
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation("junit:junit:4.13.2")
}
