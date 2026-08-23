plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.zeubicardgames.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zeubicardgames.brawl"
        minSdk = 26
        targetSdk = 36
        versionCode = 101
        versionName = "1.0.1-rift"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room { schemaDirectory("$projectDir/schemas") }

ksp { arg("room.generateKotlin", "true") }

hilt { enableAggregatingTask = true }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val prepareRiftAssets by tasks.registering {
    val sourceDir = rootProject.file("rift_assets_src")
    val outDir = file("src/main/assets/rift")
    val parts = listOf(
        sourceDir.resolve("chunk_00.b64"),
        sourceDir.resolve("chunk_01.b64"),
        sourceDir.resolve("tail_00.b64"),
        sourceDir.resolve("tail_01.b64"),
        sourceDir.resolve("tail_02.b64"),
        sourceDir.resolve("tail_03.b64"),
        sourceDir.resolve("tail_04.b64"),
        sourceDir.resolve("tail_05.b64"),
        sourceDir.resolve("tail_06.b64"),
        sourceDir.resolve("tail_07.b64"),
        sourceDir.resolve("tail_08.b64"),
        sourceDir.resolve("tail_09.b64"),
    )
    inputs.files(parts)
    outputs.dir(outDir)
    doLast {
        outDir.mkdirs()
        val base64 = parts.joinToString("") { it.readText().trim() }
        val zipBytes = java.util.Base64.getDecoder().decode(base64)
        java.util.zip.ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val target = outDir.resolve(entry.name)
                    target.parentFile.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val required = listOf("rift_sprites.webp", "sprites.json", "bg_kurokawa.webp", "menu_rift_brawl.webp")
        check(required.all { outDir.resolve(it).isFile }) { "Rift Brawl asset pack incomplete" }
    }
}

tasks.named("preBuild").configure { dependsOn(prepareRiftAssets) }
