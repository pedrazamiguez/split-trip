plugins {
    id("splittrip.jvm.library")
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
}
