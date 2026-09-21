plugins {
    id("splittrip.android.library.compose")
}

android {
    namespace = "es.pedrazamiguez.splittrip.core.designsystem"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))

    // Compose BOM — exported so all consumers align
    api(platform(libs.androidx.compose.bom))

    // Kotlinx immutable collections (used by shared UI components & UiState models)
    api(libs.kotlinx.collections.immutable)

    // Glassmorphism (blur/haze effects) — api because HazeState appears in horizonGlassEffect signature
    api(libs.haze)

    // Biometric authentication
    api(libs.androidx.biometric)

    // Compose essentials
    api(libs.androidx.ui)
    api(libs.androidx.material3)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.graphics.shapes)

    // Debug tooling (only for dev/test - not included in release APK)
    debugApi(libs.androidx.ui.tooling)
    debugApi(libs.androidx.ui.tooling.preview)
    debugApi(libs.androidx.ui.test.manifest)

    // Navigation
    api(libs.androidx.navigation.compose)

    // DI integration
    api(libs.koin.android)
    api(libs.koin.compose)

    // Image loading
    implementation(libs.coil.compose)

    // QR Code generation
    implementation(libs.zxing)

    // Google Mobile Ads & UMP
    api(libs.play.services.ads)
    api(libs.user.messaging.platform)

    // Unit Testing (extras — common test deps provided by convention plugin)
    testImplementation(libs.koin.test)
}
