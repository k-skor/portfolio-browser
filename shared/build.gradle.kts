import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

kotlin {
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

secrets {
    propertiesFileName = "environment.local"
    defaultPropertiesFileName = "environment.default"
}

tasks.withType(KotlinCompile::class.java) {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}