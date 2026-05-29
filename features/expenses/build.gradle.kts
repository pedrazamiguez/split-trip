plugins {
    id("splittrip.android.feature")
}

android {
    namespace = "es.pedrazamiguez.splittrip.features.expense"
}

dependencies {
    // Image loading for receipt photos
    implementation(libs.coil.compose)

    // Morphing polygon shapes for the receipt analysis overlay animation
    implementation(libs.androidx.graphics.shapes)
}
