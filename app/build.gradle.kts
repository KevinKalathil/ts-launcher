plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // Use the alias, remove the version "2.3.20"
}

android {
    namespace = "com.app.timespentlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.timespentlauncher"
        minSdk = 34
        targetSdk = 36
        versionCode = 3
        versionName = "3.0"

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
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
}

dependencies {

    // Core (use libs catalog versions, drop the hardcoded duplicates)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")


    // Compose BOM — controls all compose library versions consistently
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation") // HorizontalPager + combinedClickable
    implementation("androidx.compose.ui:ui-text-google-fonts:1.10.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Activity + ViewModel
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended")

    // Room (uncomment when ready)
    // implementation("androidx.room:room-runtime:2.6.1")
    // implementation("androidx.room:room-ktx:2.6.1")
    // kapt("androidx.room:room-compiler:2.6.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}