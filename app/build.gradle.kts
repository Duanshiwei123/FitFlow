plugins {
    id("com.android.application")
    // AGP 9.x 已内置 Kotlin 支持（built-in Kotlin），不要再单独应用 org.jetbrains.kotlin.android，
    // 否则会报 "Cannot add extension with name 'kotlin'"。
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fitflow.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fitflow.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Reorderable 是 KMP 库，只发布了 debug 变体、没有 release 变体。
            // 不回退的话 Gradle 变体匹配会找不到 release 构件导致 Sync/构建失败。
            matchingFallbacks += "debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // built-in Kotlin 下 Kotlin 的 jvmTarget 默认跟随 compileOptions.targetCompatibility(17)，无需 kotlinOptions{}
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 9/2 升级时 AS 模板的 libs.versions.toml 指定 composeBom=2026.02.01，此处对齐。
    // EditorScreen 用到较新 Compose API（HapticFeedbackType.SegmentFrequentTick/GestureEnd 等、
    // longPressDraggableHandle），旧 BOM(2024.12.01) 里不存在，会编译不过。
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    // animateDpAsState/animateFloatAsState 在 animation 库，material3 不传递它，需显式声明
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // 丝滑拖拽排序：长按卡片拖动 + 平滑让位动画（Calvin-LL/Reorderable）
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
}
