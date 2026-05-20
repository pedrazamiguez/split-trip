plugins {
    id("splittrip.jvm.library")
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    // Unit Testing (extras — common test deps provided by convention plugin)
    testImplementation(libs.koin.test)
}
