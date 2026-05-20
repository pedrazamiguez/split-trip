plugins {
    id("splittrip.android.library")
}

android {
    namespace = "es.pedrazamiguez.splittrip.data"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {

        getByName("debug") {
            // ** EXCHANGE_RATES_CACHE_DURATION_HOURS **
            buildConfigField(
                "long",
                "EXCHANGE_RATES_CACHE_DURATION_HOURS",
                "1L"
            )
        }

        getByName("release") {
            // ** EXCHANGE_RATES_CACHE_DURATION_HOURS **
            buildConfigField(
                "long",
                "EXCHANGE_RATES_CACHE_DURATION_HOURS",
                "24L"
            )
        }
    }
}

dependencies {
    implementation(project(":domain"))
    api(project(":data:firebase"))
    api(project(":data:local"))
    api(project(":data:remote"))

    // Other dependencies
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.timber)

    // Unit Testing (extras — common test deps provided by convention plugin)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.koin.test)
}
