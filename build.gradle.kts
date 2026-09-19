// Project root build.gradle.kts
// 📁 Inside the root build.gradle.kts file
plugins {
    // ✅ Apply the targeted 8.8.0 version globally here once
    alias(libs.plugins.android.application) version "8.8.0" apply false

    alias(libs.plugins.kotlin.android) version "2.0.21" apply false
    alias(libs.plugins.kotlin.compose) version "2.0.21" apply false
    alias(libs.plugins.ksp) version "2.0.21-1.0.26" apply false

    alias(libs.plugins.hilt) version "2.51.1" apply false
    alias(libs.plugins.googleServices) version "4.4.2" apply false
}

