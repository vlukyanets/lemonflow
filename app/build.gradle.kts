plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "io.github.vlukyanets.lemonflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.vlukyanets.lemonflow"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // Lists en/ru/uk in the system's per-app language screen (FR-022, research R4).
    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric's SDK 36 sandbox touches FileDescriptor internals via SharedSecrets.
            it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Dependency and SDK upgrades are proposed by Dependabot, not reported by every lint run.
        disable +=
            setOf(
                "GradleDependency",
                "NewerVersionAvailable",
                "AndroidGradlePluginVersion",
                "OldTargetApi"
            )
    }
}

kotlin {
    jvmToolchain(21)
}

/**
 * Copies the app's license (root `LICENSE`, GPL-3.0) and the bundled components' licenses
 * (`licenses/`) into `assets/licenses/` (FR-024, research R11), so each text has one
 * source in the repository.
 */
abstract class LicenseAssetsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val licenseFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = outputDir.get().dir("licenses").asFile
        target.deleteRecursively()
        target.mkdirs()
        licenseFiles.forEach { file ->
            val name = if (file.name == "LICENSE") "GPL-3.0.txt" else file.name
            file.copyTo(target.resolve(name), overwrite = true)
        }
    }
}

val licenseAssets = tasks.register<LicenseAssetsTask>("licenseAssets") {
    licenseFiles.from(rootProject.file("LICENSE"), rootProject.fileTree("licenses"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            licenseAssets,
            LicenseAssetsTask::outputDir
        )
    }
}

ksp {
    // Room schema JSON is committed so schema changes show up in review and migrations can be tested.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)

    testImplementation(composeBom)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
