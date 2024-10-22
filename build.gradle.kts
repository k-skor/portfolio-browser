import java.io.FileInputStream
import java.util.Properties

plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}

buildscript {
    dependencies {
        classpath(libs.secrets.gradle.plugin)
    }
}

// TODO: replace with https://github.com/google/secrets-gradle-plugin
tasks.register("setupLocalEnv") {
    val envFile = file("environment.local")
    val props = Properties()
    val isCI = providers.gradleProperty("isCI")

    if (!isCI.isPresent || isCI.get() == "false") {
        if (envFile.exists()) {
            props.load(FileInputStream(envFile))
        }
    }
    for (prop in props) {
        val key = prop.key as? String
        key?.let {
            if (project.hasProperty(key)) {
                println("has property!")
            }
        }
        project.extra[key ?: continue] = prop.value
        println("Set env $key=${prop.value}")
    }

    if (project.hasProperty("githubApiKey")) {
        println("has property!")
    }
    val githubApiKey: String by project
    println("github ext $githubApiKey")
}
