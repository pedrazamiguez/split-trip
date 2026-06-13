plugins {
    id("splittrip.android.library")
}

android {
    namespace = "es.pedrazamiguez.splittrip.core.common"
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.timber)

    // Unit testing (extras — common test deps provided by convention plugin)
    testImplementation(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.koin.test)
}
