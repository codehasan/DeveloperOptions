plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.codehasan.developeroptions"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.codehasan.developeroptions"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1001
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
}