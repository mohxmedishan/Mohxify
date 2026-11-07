plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mohx.mohxify"
    compileSdk = 35

    signingConfigs {
        create("permanentDebug") {
            storeFile = file("mohxify-debug.keystore")
            // Must match generate-keystore.yml exactly.
            storePassword = "mohxify-debug-password"
            keyAlias = "mohxify"
            keyPassword = "mohxify-debug-password"
            storeType = "JKS"
        }
    }

    defaultConfig {
        applicationId = "com.mohx.mohxify"
        minSdk = 26
        targetSdk = 29
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("permanentDebug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
