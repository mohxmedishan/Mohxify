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
            // Temporary compatibility path for existing installs. This public legacy
            // key must not be used for future public production releases.
            storePassword = "mohxify-debug-password"
            keyAlias = "mohxify"
            keyPassword = "mohxify-debug-password"
            storeType = "JKS"
        }
        create("privateRelease") {
            val keyPath = System.getenv("MOHXIFY_KEYSTORE_PATH")
            storeFile = if (keyPath.isNullOrBlank()) file("missing-release-key.jks") else rootProject.file(keyPath)
            storePassword = System.getenv("MOHXIFY_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("MOHXIFY_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("MOHXIFY_KEY_PASSWORD") ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.mohx.mohxify"
        minSdk = 26
        targetSdk = 35
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("permanentDebug")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("privateRelease")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
