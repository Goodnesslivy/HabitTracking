// 📁 Inside app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.example.habittracking"
    // ⬇️ ALTERED: Shifted down to a highly stable platform version to bypass the strict SDK 35 build rules
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.habittracking"
        minSdk = 24
        // ⬇️ ALTERED: Aligned matching stable target boundaries
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = false
        unitTests.isIncludeAndroidResources = false
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// ✅ FIXED: Substitutes the internal bytecode manipulation libraries globally to a stable
// version configuration, preventing Gradle 9.7.1 from hitting any missing Node exceptions.
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.objectweb.asm:asm-tree")).using(module("org.objectweb.asm:asm-tree:9.6"))
        substitute(module("org.objectweb.asm:asm")).using(module("org.objectweb.asm:asm:9.6"))
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // --- Core / Compose ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // --- ViewModel ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // --- Coroutines (aligned version matching) ---
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // --- Firebase: Auth + Firestore ---
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // --- Hilt (dependency injection) ---
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Calendar view (visual calendar with per-habit colored dots) ---
    implementation("com.kizitonwose.calendar:compose:2.6.0")

    // --- WorkManager ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // --- Lightweight Coroutines Test Core (Required for manual validator) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
