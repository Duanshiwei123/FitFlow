// FitFlow 根构建脚本 —— AGP 9.x + built-in Kotlin（与 D 盘工程保持一致）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
