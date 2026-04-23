plugins {
    kotlin("jvm") version "2.0.21"
}

group = "org.iesra.revilofe"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:6.0.0.M4")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.0.0.M4")
    testImplementation("io.mockk:mockk:1.14.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}