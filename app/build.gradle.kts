import java.util.Properties

plugins { id("com.android.application") }
val appVersion = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
android {
    namespace = "com.geminipocket"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.geminipocket"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("APP_VERSION_CODE")?.toInt()?.let { 300000 + it }
            ?: appVersion.getProperty("APP_VERSION_CODE").toInt()
        versionName = appVersion.getProperty("APP_VERSION_NAME") + (System.getenv("APP_VERSION_SUFFIX") ?: "")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    signingConfigs {
        if (System.getenv("RELEASE_KEYSTORE") != null) {
            create("publicRelease") {
                storeFile = file(System.getenv("RELEASE_KEYSTORE"))
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "release"
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("RELEASE_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("publicRelease")
            }
        }
    }
}
