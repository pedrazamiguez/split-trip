plugins {
    id("splittrip.android.feature")
}

android {
    namespace = "es.pedrazamiguez.splittrip.features.subunit"
}

dependencies {
    implementation(libs.coil.compose)
}
