plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystore: String? = System.getenv("LCKDN_KEYSTORE")

android {
    namespace = "com.lckdn.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lckdn.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 22
        versionName = "1.5.2"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null && file(releaseKeystore).exists()) {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("LCKDN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LCKDN_KEY_ALIAS")
                keyPassword = System.getenv("LCKDN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseKeystore != null && file(releaseKeystore).exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("com.google.android.material:material:1.12.0")
}
