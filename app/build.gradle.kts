// App module build file.
// No Firebase, no Google Play Services, no analytics SDKs (ADR 0001).
// INTERNET is used only for Link import and Check for updates (ADR 0002).
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing details live in keystore.properties, which is never committed.
// Without that file, release builds come out unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "io.github.bijojp.weekendvibes"
    // Current Compose needs API 37 to compile against; targetSdk below is what Play requires.
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.bijojp.weekendvibes"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Installs side by side with a release build so testing never touches real data.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        // targetSdk stays at what Play requires (36) until we choose to adopt the next one.
        disable += "OldTargetApi"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    // Compose BOM keeps Compose artifacts aligned.
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation3:navigation3-runtime:1.2.0")
    implementation("androidx.navigation3:navigation3-ui:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    // Pictures are local files; Coil only decodes and caches them (no network module).
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
