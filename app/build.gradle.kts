plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.immersa.vr"
 compileSdk = 35
 defaultConfig { applicationId = "com.immersa.vr"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0-alpha" }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
