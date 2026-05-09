plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gustavo.brilhante.cutestickers.network"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    
    api(libs.retrofit)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
}
