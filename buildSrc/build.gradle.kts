plugins {
    `java-library`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    val gradleLibDir = gradle.gradleHomeDir?.resolve("lib")
            ?: error("Gradle home directory is required to locate bundled test libs")
    testImplementation(files(gradleLibDir.resolve("junit-4.13.2.jar")))
    testImplementation(files(gradleLibDir.resolve("hamcrest-core-1.3.jar")))
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
