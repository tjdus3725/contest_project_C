plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val pleosNavi = (project.findProperty("pleosNavi") as String?) != "false"

android {
    namespace = "com.contest.dongheng"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.contest.dongheng"
        minSdk = 28 // Pleos Connect 요구사항: API 28 이상
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    sourceSets["main"].java.srcDir("src/main/kotlin")
    if (pleosNavi) sourceSets["main"].java.srcDir("src/pleos/kotlin")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    if (pleosNavi) implementation("ai.pleos.playground:NaviHelper:3.0.5")
}
