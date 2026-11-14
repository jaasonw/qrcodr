plugins { id("com.android.application") version "9.5.1" }

android {
  namespace = "io.github.jaasonw.qrcodr"
  compileSdk = 36

  defaultConfig {
    applicationId = "io.github.jaasonw.qrcodr"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  // Release key lives outside the repo, in ~/.gradle/gradle.properties. Without it the release
  // APK is built unsigned.
  val storeFile = providers.gradleProperty("qrcodr.storeFile").orNull
  if (storeFile != null) {
    signingConfigs.create("release") {
      this.storeFile = file(storeFile)
      storePassword = providers.gradleProperty("qrcodr.storePassword").get()
      keyAlias = providers.gradleProperty("qrcodr.keyAlias").get()
      keyPassword = providers.gradleProperty("qrcodr.keyPassword").get()
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.findByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }
}

dependencies {
  implementation("com.google.zxing:core:3.5.4")
  testImplementation("junit:junit:4.13.2")
}
