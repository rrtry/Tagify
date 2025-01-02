plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.rrtry.tagify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rrtry.tagify"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    val lifecycle_version = "2.7.0"
    val nav_version  = "2.7.6"
    val room_version = "2.6.1"
    val hilt_version = "2.48"

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.room:room-runtime:$room_version")
    implementation("com.google.dagger:hilt-android:$hilt_version")
    implementation("androidx.room:room-ktx:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    ksp("androidx.room:room-compiler:$room_version")
    ksp("com.google.dagger:hilt-compiler:$hilt_version")

    implementation(files("libs/ffmpeg-kit-x86.aar"))
    implementation(files("libs/JTagger.jar"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation ("androidx.navigation:navigation-compose:$nav_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0-rc01")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}