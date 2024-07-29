group = "com.sirnuke.elusivebot"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("com.diffplug.spotless") version "6.25.0"
    `java-library`
    `maven-publish`
}

spotless {
    kotlin {
        diktat()
    }
    kotlinGradle {
        diktat()
    }
}

repositories {
    mavenCentral()
}

run {
    if (project.hasProperty("internalMavenUrl")) {
        val internalMavenUsername: String by project
        val internalMavenPassword: String by project
        val internalMavenUrl: String by project

        publishing {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
            repositories {
                maven {
                    credentials {
                        username = internalMavenUsername
                        password = internalMavenPassword
                    }
                    val releasesRepoUrl = "$internalMavenUrl/releases/"
                    val snapshotsRepoUrl = "$internalMavenUrl/snapshots/"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                    name = "Internal-Maven-Publish"
                }
            }
        }

        repositories {
            maven {
                credentials {
                    username = internalMavenUsername
                    password = internalMavenPassword
                }
                url = uri("$internalMavenUrl/releases")
                name = "Internal-Maven-Releases"
            }
        }

        repositories {
            maven {
                credentials {
                    username = internalMavenUsername
                    password = internalMavenPassword
                }
                url = uri("$internalMavenUrl/snapshots")
                name = "Internal-Maven-Snapshots"
            }
        }
    } else {
        repositories {
            mavenLocal()
        }
        publishing {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
    }
}

val kafkaApiVersion = "3.6.1"

dependencies {
    api("org.slf4j:slf4j-api:2.0.11")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    api("org.apache.kafka:kafka-streams:$kafkaApiVersion")
    api("org.apache.kafka:kafka-clients:$kafkaApiVersion")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaApiVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
