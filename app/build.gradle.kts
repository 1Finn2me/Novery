plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.emptycastle.novery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.emptycastle.novery"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }

    buildFeatures {
        compose = true
    }

    // Ensure the Compose compiler plugin version is set to match your Compose libraries
    composeOptions {
        // Pick a compiler extension version compatible with your Compose UI / Material versions
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.palette.ktx)

    implementation(libs.androidx.compose.foundation)
    implementation("androidx.compose.foundation:foundation:1.7.0")
    // System UI Controller
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.compose.animation)
    ksp("androidx.room:room-compiler:2.6.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.animation:animation:1.6.1")
    implementation("androidx.compose.animation:animation-core:1.6.1")

    // EPUB Generation
    implementation("org.redundent:kotlin-xml-builder:1.9.1")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // For MediaStyle notifications
    implementation("androidx.media:media:1.7.0")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("sh.calvin.reorderable:reorderable:2.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
