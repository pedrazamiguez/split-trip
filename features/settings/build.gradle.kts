plugins {
    id("splittrip.android.feature")
}

android {
    namespace = "es.pedrazamiguez.splittrip.features.settings"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
