import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.Duration

plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.freefair.lombok") version "9.7.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "net.guizhanss"

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.alessiodp.com/releases/")
        maven("https://repo.xenondevs.xyz/releases/")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        api("com.google.code.findbugs:jsr305:3.0.2")
        // compile against folia-api (superset of paper-api with regionized schedulers) for Folia compatibility
        compileOnly("dev.folia:folia-api:26.3.build.26-stable")
        // keep in sync with the Paper API build MockBukkit 26.3 is built against
        testImplementation("io.papermc.paper:paper-api:26.3.build.200-stable")
        testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:6.1.3")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
        testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.3:4.123.0")
    }

    java {
        disableAutoTargetJvm()
        // Minecraft 26.3 (Paper/Folia) requires Java 25
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<ShadowJar> {
        archiveClassifier = ""
    }

    tasks.named<Jar>("jar") {
        archiveClassifier = "original"
    }

    tasks.named("build") {
        dependsOn(tasks.named("shadowJar"))
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["shadow"])

                artifact(tasks.named("javadocJar").get())
                artifact(tasks.named("sourcesJar").get())

                groupId = rootProject.group as String
                artifactId = project.name
                version = rootProject.version as String

                pom {
                    name.set("guizhanlib")
                    description.set("A library for Slimefun addon development.")
                    url.set("https://github.com/ybw0014/guizhanlib")

                    licenses {
                        license {
                            name.set("GPL-3.0 license")
                            url.set("https://github.com/ybw0014/guizhanlib/blob/master/LICENSE")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            name.set("ybw0014")
                            url.set("https://ybw0014.dev/")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/ybw0014/guizhanlib.git")
                        developerConnection.set("scm:git:ssh://github.com:ybw0014/guizhanlib.git")
                        url.set("https://github.com/ybw0014/guizhanlib/tree/master")
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["maven"])
    }
}

tasks.named("publish") {
    dependsOn(tasks.named("publishToSonatype"))
    dependsOn(tasks.named("closeAndReleaseSonatypeStagingRepository"))
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
    // Central Portal validation can take several minutes on a multi-module build
    transitionCheckOptions {
        maxRetries = 240
        delayBetween = Duration.ofSeconds(5)
    }
}
