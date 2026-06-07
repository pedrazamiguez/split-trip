plugins {
    id("splittrip.android.library")
}

android {
    namespace = "es.pedrazamiguez.splittrip.data.remote"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {

        getByName("debug") {
            // ** OER_API_BASE_URL **
            buildConfigField(
                "String",
                "OER_API_BASE_URL",
                "\"https://openexchangerates.org/api/\""
            )

            // ** OER_APP_ID **
            val appId =
                providers.gradleProperty("OER_APP_ID_DEBUG").orNull ?: "YOUR_DEBUG_OER_APP_ID"
            buildConfigField(
                "String",
                "OER_APP_ID",
                "\"$appId\""
            )

            val displayedAppId = if (appId == "YOUR_DEBUG_OER_APP_ID") {
                appId // Don't shade the placeholder
            } else if (appId.length > 7) { // Ensure enough length to show first 3 and last 4
                "${appId.take(3)}...${appId.takeLast(4)}"
            } else if (appId.length > 4) { // If not long enough for first 3 + last 4, just show last 4
                "...${appId.takeLast(4)}"
            } else { // For very short IDs, shade most of it
                if (appId.isNotEmpty()) "*".repeat(appId.length) else "..."
            }
            logger.lifecycle("Open Exchange Rates App ID for debug: $displayedAppId")
        }

        getByName("release") {
            // ** OER_API_BASE_URL **
            buildConfigField(
                "String",
                "OER_API_BASE_URL",
                "\"https://openexchangerates.org/api/\""
            )

            // ** OER_APP_ID **
            val appIdFromEnv = providers.environmentVariable("OER_APP_ID_RELEASE").orNull
            val appIdFromGradleProps = providers.gradleProperty("OER_APP_ID_RELEASE").orNull
            val isCI = providers.environmentVariable("CI").orNull?.toBoolean() == true
            val appId = appIdFromEnv ?: appIdFromGradleProps ?: run {
                if (isCI) {
                    throw GradleException(
                        "Open Exchange Rates App ID for release (OER_APP_ID_RELEASE) not found in environment variables or gradle properties."
                    )
                } else {
                    "YOUR_RELEASE_OER_APP_ID"
                }
            }

            buildConfigField(
                "String",
                "OER_APP_ID",
                "\"$appId\""
            )
            logger.lifecycle(
                "Open Exchange Rates App ID for release source: ${if (appIdFromEnv != null) {
                    "ENV VAR"
                } else if (appIdFromGradleProps != null) {
                    "GRADLE PROP"
                } else {
                    "PLACEHOLDER / ERROR"
                }}"
            )
        }
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(project(":core:logging"))

    // Unit Testing (extras — common test deps provided by convention plugin)
    testImplementation(libs.koin.test)
}
