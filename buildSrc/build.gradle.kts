plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    testImplementation("junit:junit:4.13.2")
}
