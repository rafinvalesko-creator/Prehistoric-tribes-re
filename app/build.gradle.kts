plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.ancienttribes.game"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ancienttribes.game"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
kotlin { jvmToolchain(17) }
