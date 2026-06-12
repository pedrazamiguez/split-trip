plugins {
    id("splittrip.android.library")
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "es.pedrazamiguez.splittrip.data.local"

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Unit Testing (extras — common test deps provided by convention plugin)
    testImplementation(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.koin.test)
}

ksp {
    arg(
        "room.incremental",
        "true"
    )
    arg(
        "room.expandProjection",
        "true"
    )
}
