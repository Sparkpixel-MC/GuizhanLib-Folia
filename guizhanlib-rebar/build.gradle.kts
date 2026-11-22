dependencies {
    api(project(":guizhanlib-common", configuration = "shadow"))
    api(project(":guizhanlib-localization", configuration = "shadow"))
    api(project(":guizhanlib-minecraft", configuration = "shadow"))
    compileOnly("io.github.pylonmc:rebar:0.35.0")
    testImplementation("io.github.pylonmc:rebar:0.35.0")
}
