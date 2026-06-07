plugins {
    id("splittrip.android.library")
}

android {
    namespace = "es.pedrazamiguez.splittrip.core.logging"
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.timber)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
