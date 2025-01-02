import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.koin)
            implementation(libs.ktor.cio)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.json.serialization)
            implementation(libs.kotlinx.serialization)
            implementation(libs.orbitmvi.core)
            implementation(libs.appcash.paging.common)
            implementation(libs.kvault)
        }
        androidMain.dependencies {
            // Logging
            implementation(libs.kotlin.logging)
            // Firebase
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.auth.ktx)
            implementation(libs.firebase.firestore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "pl.krzyssko.portfoliobrowser"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    secrets {
        propertiesFileName = "secrets.local"
        defaultPropertiesFileName = "secrets.default"
    }
}

// Recipe: https://github.com/android/gradle-recipes/tree/agp-8.7/addCustomBuildConfigFields#adding-fields
androidComponents {
    onVariants {
        it.buildConfigFields.put("TAG", BuildConfigField("String", "\"${rootProject.name}\"", "Logging tag"))
    }
}

tasks.withType(KotlinCompile::class.java) {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}