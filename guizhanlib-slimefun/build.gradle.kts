dependencies {
    api(project(":guizhanlib-common", configuration = "shadow"))
    api(project(":guizhanlib-localization", configuration = "shadow"))
    api(project(":guizhanlib-minecraft", configuration = "shadow"))
    // jitpack no longer serves com.github.Slimefun artifacts, use the GuguProject fork build
    compileOnly("com.github.SlimefunGuguProject:Slimefun4:2024.3.1")
}
