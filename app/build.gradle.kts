import com.lumen.build.SanitizePromptHeaderTransform
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lumen"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lumen"
        minSdk = 26
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
    implementation("com.google.android.material:material:1.9.0")
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

val promptHeaderSanitizedAttribute = Attribute.of("promptHeaderSanitized", Boolean::class.javaObjectType)

dependencies {
    attributesSchema.attribute(promptHeaderSanitizedAttribute)
    artifactTypes.maybeCreate("aar").attributes.attribute(promptHeaderSanitizedAttribute, false)
    registerTransform(SanitizePromptHeaderTransform::class) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
        from.attribute(promptHeaderSanitizedAttribute, false)
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
        to.attribute(promptHeaderSanitizedAttribute, true)
    }
}

configurations.configureEach {
    if (isCanBeResolved) {
        attributes.attribute(promptHeaderSanitizedAttribute, true)
    }
}
