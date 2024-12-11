plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    id("com.google.gms.google-services")
}

android {
    namespace = "pl.krzyssko.portfoliobrowser.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "pl.krzyssko.portfoliobrowser.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation)
    implementation(libs.orbitmvi.core)
    implementation(libs.orbitmvi.compose)
    implementation(libs.koin.android)
    implementation(libs.appcash.paging.common)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.compose.android)
    implementation(libs.coil)
    implementation(libs.coil.network)
    implementation(libs.coil.svg)
    //implementation(files("slf4j-simple-2.0.9"))
    //implementation(libs.logging.backend)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.koin.test)
}
