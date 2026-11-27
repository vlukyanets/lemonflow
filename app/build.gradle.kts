plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries.android)
}

android {
    namespace = "io.github.vlukyanets.lemonflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.vlukyanets.lemonflow"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Generates R.raw.aboutlibraries (third-party licenses shown in the app) and fails the
// build when a dependency uses a license outside the allow-list (see AGENTS.md).
aboutLibraries {
    offlineMode = true
    collect {
        configPath = file("aboutlibraries")
        includePlatform = false
    }
    license {
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "ISC")
        // SQLCipher's POM names no license; its text (BSD-3-Clause) is in aboutlibraries/licenses.
        allowedLicensesMap = mapOf("SQLCipher Community Edition License" to listOf("net.zetetic"))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)

    implementation(libs.aboutlibraries.compose.m3)
}
