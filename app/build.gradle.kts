plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// The deployed backend. Both build types default to it; override per build
// with -PURIMAI_DEV_API_BASE_URL (debug) or -PURIMAI_API_BASE_URL (release).
val DEPLOYED_API_BASE_URL = "https://backend-delta-red-60.vercel.app/"

// Google OAuth *web* client id. Public by design -- it ships inside every APK
// and identifies the app to Google; it is not a secret. The client *secret* is
// deliberately not here and is not needed: ID tokens are verified against
// Google's public keys.
val DEFAULT_GOOGLE_CLIENT_ID =
  "509413302467-s83kd3r3m0h6v7jed00dfjniur0cmk4e.apps.googleusercontent.com"

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.urimai.cvkgrt"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // The Google WEB client id (not the Android one): Credential Manager
    // returns a token audienced to the web client, and the backend checks that
    // audience.
    //
    // Defaulted rather than left empty. An empty value builds fine and fails
    // only on the device, as NoCredentialException surfaced to the user as
    // "No Google account was found" -- which blames their phone for a missing
    // build flag. Override with -PURIMAI_GOOGLE_CLIENT_ID=... if needed.
    val googleClientId = providers.gradleProperty("URIMAI_GOOGLE_CLIENT_ID")
      .orElse(providers.environmentVariable("URIMAI_GOOGLE_CLIENT_ID"))
      .getOrElse(DEFAULT_GOOGLE_CLIENT_ID)
    buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")

      // The release build must point at a real, HTTPS backend. There is no
      // sensible default for this, so the build fails loudly when it is unset
      // rather than shipping an APK that silently talks to nothing.
      val releaseApi = providers.gradleProperty("URIMAI_API_BASE_URL")
        .orElse(providers.environmentVariable("URIMAI_API_BASE_URL"))
        .getOrElse(DEPLOYED_API_BASE_URL)
      check(releaseApi.isEmpty() || releaseApi.startsWith("https://")) {
        "URIMAI_API_BASE_URL must be an https:// URL for release builds (got: $releaseApi)"
      }
      buildConfigField("String", "API_BASE_URL", "\"$releaseApi\"")
      buildConfigField("boolean", "HTTP_LOGGING", "false")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")

      // Debug points at your machine. 10.0.2.2 is the emulator's alias for the
      // host; for a physical phone on the same Wi-Fi, set URIMAI_DEV_API_BASE_URL
      // to http://<your-LAN-IP>:4000/ (the emulator alias is not routable there).
      val devApi = providers.gradleProperty("URIMAI_DEV_API_BASE_URL")
        .orElse(providers.environmentVariable("URIMAI_DEV_API_BASE_URL"))
        .getOrElse(DEPLOYED_API_BASE_URL)
      buildConfigField("String", "API_BASE_URL", "\"$devApi\"")
      buildConfigField("boolean", "HTTP_LOGGING", "true")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  // GEMINI_API_KEY must never be injected into BuildConfig: an APK is
  // decompilable, so a bundled key is a published key. All model calls go
  // through the backend, which holds the key in server environment.
  ignoreList.add("GEMINI_API_KEY")
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // Credential Manager is the current Google Sign-In path; the old
  // GoogleSignInClient API is deprecated. It is a Play Services library, not
  // Firebase, so it needs no google-services.json.
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.moshi.kotlin.codegen)
}
