plugins {
    id("splittrip.android.feature")
}

android {
    namespace = "es.pedrazamiguez.splittrip.features.profile"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
