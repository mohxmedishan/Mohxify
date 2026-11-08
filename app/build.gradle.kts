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
            // Legacy signing is retained temporarily to preserve update compatibility.
            // Replace this with a private release key stored in GitHub Actions secrets
            // before distributing a security-hardened public release.
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("permanentDebug")
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
