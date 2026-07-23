plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tmaster"
    compileSdk = 34

    // ── 版本号管理 ─────────────────────────────────────────
    // versionCode: 每次构建自增 (基于时间戳，永不回退)
    // versionName: 语义版本 + build 号
    val buildNumber = (System.currentTimeMillis() / 1000 / 60).toInt()
    defaultConfig {
        applicationId = "com.tmaster"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "0.1.0.$buildNumber"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // ── 签名配置 ───────────────────────────────────────────
    // 覆盖安装需要签名一致。
    // Debug: 使用系统默认 debug.keystore (每台机器固定)
    // Release: 从环境变量读取或使用本地 keystore
    signingConfigs {
        create("release") {
            // CI 环境: 从 GitHub Secrets 读取
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "tmaster"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            // Debug 使用默认 debug 签名，同机器覆盖安装无问题
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // 如果 release signingConfig 没配好，回退到 debug 签名
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile?.exists() == true) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    // ── NDK / CMake ────────────────────────────────────────
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    ndk { abiFilters += listOf("arm64-v8a") }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // gRPC
    implementation("io.grpc:grpc-okhttp:1.64.0")
    implementation("io.grpc:grpc-protobuf-lite:1.64.0")
    implementation("io.grpc:grpc-stub:1.64.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
