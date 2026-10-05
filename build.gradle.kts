import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.freefair.lombok") version "8.13.1"
    id("com.gradleup.shadow") version "8.3.6"
    id("org.sonarqube") version "6.1.0.5360"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "net.guizhanss"

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.alessiodp.com/releases/")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "org.sonarqube")

    lombok {
        version = "1.18.48" // bundled version does not support JDK 25 compilation
    }

    dependencies {
        api("com.google.code.findbugs:jsr305:3.0.2")
        // compile against folia-api (superset of paper-api with regionized schedulers) for Folia compatibility
        compileOnly("dev.folia:folia-api:26.2.build.7-beta")
        testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
        testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:6.1.3")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
    }

    java {
        disableAutoTargetJvm()
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<ShadowJar> {
        archiveClassifier = ""
    }

    sonar {
        properties {
            property("sonar.projectKey", "ybw0014_GuizhanLib")
            property("sonar.organization", "ybw0014")
            property("sonar.host.url", "https://sonarcloud.io")
        }
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}
