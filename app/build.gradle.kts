plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.compose) }
android {
 namespace = "com.jiwei.collection"
 compileSdk = 37
 defaultConfig { applicationId = "com.jiwei.collection"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
dependencies {
 implementation(platform(libs.androidx.compose.bom))
 implementation(libs.androidx.activity.compose)
 implementation(libs.androidx.compose.material3)
 implementation(libs.androidx.compose.ui)
 implementation(libs.androidx.compose.ui.graphics)
 implementation(libs.androidx.compose.ui.tooling.preview)
 implementation(libs.androidx.core.ktx)
 implementation(libs.coil.compose)
 debugImplementation(libs.androidx.compose.ui.tooling)
}
