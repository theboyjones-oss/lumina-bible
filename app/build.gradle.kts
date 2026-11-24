plugins {
    id("com.android.application")
}

android {
    namespace = "com.lumina.biblestudy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lumina.biblestudy"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            storeFile = file("lumina-release.jks")
            // Keystore is password-protected; the password lives only in the KEYSTORE_PASSWORD repo secret
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "lumina"
            keyPassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Align Kotlin stdlib versions pulled in by AndroidX (avoids duplicate-class errors)
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
