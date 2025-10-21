import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun File.replaceInvalidPromptHeaderStrings(logger: Logger): Boolean {
    if (!exists()) {
        return false
    }
    var patchedAny = false
    val token = "\"{str}\""
    walkTopDown()
        .filter { it.isFile && it.name == "values.xml" }
        .forEach { file ->
            val original = file.readText()
            if (token in original) {
                val patched = original.replace(token, "\"%1\$s\"")
                if (patched != original) {
                    file.writeText(patched)
                    logger.info("Patched invalid prompt_header resource in ${file.absolutePath}")
                    patchedAny = true
                }
            }
        }
    return patchedAny
}

fun Project.sanitizePromptHeaderCaches() {
    val projectLogger = this.logger
    layout.buildDirectory.get().asFile.replaceInvalidPromptHeaderStrings(projectLogger)

    val cachesDir = File(gradle.gradleUserHomeDir, "caches")
    if (cachesDir.exists()) {
        cachesDir
            .listFiles { file -> file.isDirectory && file.name.startsWith("transforms-") }
            ?.forEach { transformDir ->
                transformDir.replaceInvalidPromptHeaderStrings(projectLogger)
            }
    }
}

android {
    namespace = "com.lumen"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lumen"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.3")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val gradleProject = project

android.applicationVariants.all {
    mergeResourcesProvider.configure {
        doFirst {
            gradleProject.sanitizePromptHeaderCaches()
            // Patch any dependency resource inputs that still ship the malformed prompt string
            inputs.files
                .filter { it.exists() }
                .forEach { input ->
                    if (input.isDirectory) {
                        input.replaceInvalidPromptHeaderStrings(gradleProject.logger)
                    } else if (input.isFile && input.extension.equals("aar", ignoreCase = true)) {
                        // Some intermediate inputs are packaged AARs. Extract them to a temp
                        // directory, sanitize the resources, then repack before aapt consumes
                        // them so the merge step sees a valid string definition.
                        val tempDir = gradleProject.layout.buildDirectory
                            .dir("patched-aar/${input.nameWithoutExtension}")
                            .get()
                            .asFile
                        if (tempDir.exists()) {
                            tempDir.deleteRecursively()
                        }
                        tempDir.mkdirs()
                        try {
                            gradleProject.copy {
                                from(gradleProject.zipTree(input))
                                into(tempDir)
                            }
                            val didPatch = tempDir.replaceInvalidPromptHeaderStrings(gradleProject.logger)
                            if (didPatch) {
                                if (input.exists() && !input.delete()) {
                                    throw GradleException("Unable to replace ${input.absolutePath} with sanitized resources")
                                }
                                gradleProject.ant.invokeMethod(
                                    "zip",
                                    mapOf(
                                        "destfile" to input.absolutePath,
                                        "basedir" to tempDir.absolutePath
                                    )
                                )
                            }
                        } finally {
                            tempDir.deleteRecursively()
                        }
                    }
                }
        }
    }
}
