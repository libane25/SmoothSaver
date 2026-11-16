plugins {
    id("com.android.application")
}

android {
    namespace = "com.smoothsaver.xposed"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smoothsaver.xposed"
        minSdk = 34
        targetSdk = 35
        versionCode = 5
        versionName = "1.3.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Disable lint for faster builds
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Legacy Xposed API for broader compatibility (using local jar to avoid repository issues)
    compileOnly(files("libs/api-82.jar"))
    testImplementation("junit:junit:4.13.2")
}
