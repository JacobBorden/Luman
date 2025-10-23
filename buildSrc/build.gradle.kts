plugins {
    `java-library`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    testImplementation("junit:junit:4.13.2")
}
