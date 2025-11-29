import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Load local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "kr.ac.dongyang.mobileproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.ac.dongyang.mobileproject"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose properties to BuildConfig
        buildConfigField("String", "DB_HOST", "\"${localProperties.getProperty("db.host")}\"")
        buildConfigField("String", "DB_NAME", "\"${localProperties.getProperty("db.name")}\"")
        buildConfigField("String", "DB_USER", "\"${localProperties.getProperty("db.user")}\"")
        buildConfigField("String", "DB_PASSWORD", "\"${localProperties.getProperty("db.password")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.mysql.connector.java)
    implementation(libs.material.v190)
    implementation(libs.cardview)
    implementation(libs.coordinatorlayout)
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}