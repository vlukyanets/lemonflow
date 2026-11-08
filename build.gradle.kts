plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied (AGP 9 has built-in Kotlin); declared only to pin the Kotlin Gradle plugin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        // .claude/ holds feature worktrees - full checkouts that must not be formatted from here.
        targetExclude("**/build/**", ".claude/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", ".claude/**")
        ktlint(libs.versions.ktlint.get())
    }
}
