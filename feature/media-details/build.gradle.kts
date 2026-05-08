plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "com.gustavo.brilhante.cutecats.feature.mediadetails"
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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":feature:stickers"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))

    val bom = libs.androidx.compose.bom
    implementation(platform(bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(platform(bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(platform(bom))
    debugImplementation(libs.androidx.ui.test.manifest)
}
