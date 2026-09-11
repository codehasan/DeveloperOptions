import com.android.build.api.variant.impl.VariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing is optional: absent keystore.properties (e.g. a fresh clone or CI),
// the release signingConfig is skipped so debug builds still work.
val keystorePropertiesFile: File = rootProject.file("app/keystore.properties")
val keystoreProperties: Properties? = keystorePropertiesFile.takeIf { it.exists() }?.let {
    Properties().apply { FileInputStream(it).use(::load) }
}

android {
    namespace = "io.github.codehasan.developeroptions"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.codehasan.developeroptions"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1002
        versionName = "2.0"
    }

    signingConfigs {
        keystoreProperties?.let { props ->
            create("release") {
                storeFile = props["storeFile"]?.let { file(it) }
                storePassword = props["storePassword"].toString()
                keyAlias = props["keyAlias"].toString()
                keyPassword = props["keyPassword"].toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) {
                output.outputFileName.set("Developer_Options.apk")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
}