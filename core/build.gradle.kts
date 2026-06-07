plugins {
    id("splittrip.android.library")
}

android {
    namespace = "es.pedrazamiguez.splittrip.core"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:design-system"))
    api(project(":core:logging"))
}
