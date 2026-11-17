import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.applet.elhzku"
        minSdk = 24
        targetSdk = 36
        versionCode = 47
        versionName = "3.81"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    androidResources {
        noCompress += "gz"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")

    // ML Kit Text Recognition (Unbundled via Google Play Services to keep APK lightweight)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-japanese:16.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val appVersionName = android.defaultConfig.versionName ?: "latest"

val updateWordbookApk = tasks.register("updateWordbookApk") {
    doNotTrackState("Copies APK to workspace root and build outputs")
    val buildDir = layout.buildDirectory
    val root = layout.projectDirectory.dir("..")
    val versionTag = appVersionName
    doLast {
        val apk = buildDir.file("outputs/apk/debug/app-debug.apk").get().asFile
        if (apk.exists()) {
            val rootDir = root.asFile
            val versionedName = "wordbook_v${versionTag}.apk"
            rootDir.listFiles { _, name ->
                name == "wordbook.apk" || (name.startsWith("wordbook_v") && name.endsWith(".apk") && name != versionedName)
            }?.forEach { it.delete() }
            val versionedApk = File(rootDir, versionedName)
            apk.copyTo(versionedApk, overwrite = true)
            val buildOutputsDir = File(rootDir, ".build-outputs")
            if (buildOutputsDir.exists() || buildOutputsDir.mkdirs()) {
                apk.copyTo(File(buildOutputsDir, "app-debug.apk"), overwrite = true)
            }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(versionedApk.readBytes())
                .joinToString("") { "%02x".format(it) }
            println("Exported APK: ${versionedApk.name} (${versionedApk.length()} bytes, sha256=$digest)")
        }
    }
}

tasks.configureEach {
    if (name == "assembleDebug") {
        finalizedBy(updateWordbookApk)
    }
}
