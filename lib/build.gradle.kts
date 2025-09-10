import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "com.sd.lib.compose.swich"
  compileSdk = libs.versions.androidCompileSdk.get().toInt()
  defaultConfig {
    minSdk = 21
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
  }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.material3)
}

mavenPublishing {
  configure(
    AndroidSingleVariantLibrary(
      variant = "release",
      sourcesJar = true,
      publishJavadocJar = true,
    )
  )
}