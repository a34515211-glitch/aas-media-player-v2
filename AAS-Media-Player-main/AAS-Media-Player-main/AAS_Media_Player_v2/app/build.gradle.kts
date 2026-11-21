plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aas.mediaplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aas.mediaplayer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose + Material3
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Media3 ExoPlayer (للتشغيل في الخلفية والقوائم المنفصلة)
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media3:media3-ui:1.3.0")

    // FFmpegKit (لقص الصوت وتحويل الفيديو إلى صوت)
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2.LTS")

    // تخزين التفضيلات (لحفظ حالة كل ملف)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // إذن Runtime
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
}
