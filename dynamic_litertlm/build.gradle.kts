plugins {
    id("splittrip.android.dynamic-feature")
}

android {
    namespace = "es.pedrazamiguez.splittrip.dynamic.litertlm"

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/*.kotlin_module",
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES"
            )
        }
    }

    buildTypes {
        getByName("debug") {}
        getByName("release") {}
        create("internalRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":data"))
    implementation(libs.google.ai.edge.litertlm)
    implementation(libs.play.feature.delivery.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
}
