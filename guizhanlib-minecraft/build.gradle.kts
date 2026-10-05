dependencies {
    api(project(":guizhanlib-common", configuration = "shadow"))
    api(project(":guizhanlib-localization", configuration = "shadow"))
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
}
