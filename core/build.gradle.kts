import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: the domain model and rules. Nothing here may import Android.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Compiled against the Java 8 API, which is what Android 8 (minSdk 26) offers: a call to a newer Java
// API such as LocalDate.ofInstant (Android 14+) fails the build here instead of crashing old phones.
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=1.8")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// ReadmeExamplesTest checks the README's examples against the parser, so a README edit reruns it.
tasks.test {
    inputs.file(rootProject.file("README.md")).withPathSensitivity(PathSensitivity.RELATIVE)
}
