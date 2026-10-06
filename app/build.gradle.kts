plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.metahumanlegacy.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.metahumanlegacy.game"
        minSdk = 23
        targetSdk = 36
        versionCode = 50
        versionName = "5.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    buildTypes {
        release {
            // Keep the previously installed development app and its saves intact.
            applicationIdSuffix = ".final"
            resValue("string", "app_name", "MetaHuman Legacy Final")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.compose.ui:ui:1.11.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.4")
    implementation("androidx.compose.foundation:foundation:1.11.4")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling:1.11.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.11.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.11.4")
}
