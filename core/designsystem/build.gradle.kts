plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gustavo.brilhante.cutestickers.designsystem"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
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
    val bom = libs.androidx.compose.bom
    implementation(platform(bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
}
