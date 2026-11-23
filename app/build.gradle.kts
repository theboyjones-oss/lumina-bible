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
        versionCode = 2
        versionName = "1.1"

        // Optional: bake a Gemini key into the APK from the GEMINI_API_KEY build secret
        val geminiKey = (System.getenv("GEMINI_API_KEY") ?: "").trim().replace("\"", "")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("lumina-release.jks")
            storePassword = "lumina-bible"
            keyAlias = "lumina"
            keyPassword = "lumina-bible"
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
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
