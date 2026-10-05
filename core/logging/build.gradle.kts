plugins {
    id("splittrip.android.library")
}

android {
    namespace = "es.pedrazamiguez.splittrip.core.logging"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.timber)

    api(platform(libs.firebase.bom))
    api(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
