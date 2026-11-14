import com.android.build.api.variant.HasUnitTest
import com.android.build.api.variant.ResValue
import net.ltgt.gradle.errorprone.ErrorProneOptions
import java.util.Locale
import java.util.Properties

plugins {
  id("quran.android.application")
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.ksp)
  alias(libs.plugins.errorprone)
  alias(libs.plugins.metro)
  alias(libs.plugins.kover)
  alias(libs.plugins.sqldelight)
}

// This distribution is private by design: no analytics, crash reporting, or external accounts.
val useFirebase = false
val releaseSigning = mapOf(
  "STORE_FILE" to providers.gradleProperty("STORE_FILE").orNull,
  "STORE_PASSWORD" to providers.gradleProperty("STORE_PASSWORD").orNull,
  "KEY_ALIAS" to providers.gradleProperty("KEY_ALIAS").orNull,
  "KEY_PASSWORD" to providers.gradleProperty("KEY_PASSWORD").orNull
)
val hasReleaseSigning = releaseSigning.values.all { !it.isNullOrBlank() }
val oauthProperties = Properties().apply {
  // Optional local sync config. Android OAuth uses PKCE, so client secrets are intentionally unsupported.
  val oauthPropertiesFile = rootProject.file("oauth.properties")
  if (oauthPropertiesFile.exists()) {
    oauthPropertiesFile.inputStream().use(::load)
  }
}

// only want to apply the Firebase plugin if we're building a release build. moving this to the
// release build type won't work, since debug builds would also implicitly get the plugin.
if (getGradle().startParameter.taskRequests.toString().contains("Release") && useFirebase) {
  apply(plugin = "com.google.gms.google-services")
  apply(plugin = "com.google.firebase.crashlytics")
}

android {
  namespace = "com.quran.labs.androidquran"

  defaultConfig {
    versionCode = 10108
    versionName = "1.1.8"
    testInstrumentationRunner = "com.quran.labs.androidquran.core.QuranTestRunner"
  }

  buildFeatures.resValues = true

  androidResources {
    // Indonesian is still in instead of id due to https://issuetracker.google.com/issues/36911507
    @Suppress("UnstableApiUsage")
    localeFilters += listOf(
      "ar", "az", "bg", "bn", "bs", "cs", "da", "de", "el", "es", "et", "fa",
      "fi", "fr", "hi", "hr", "hu", "in", "it", "ja", "kk", "ko", "ku", "lt",
      "lv", "ms", "nl", "pl", "ps", "pt", "ro", "ru", "sk", "sl", "sq", "sr",
      "sv", "th", "tr", "ug", "uk", "ur", "uz", "vi", "zh"
    )
  }

  dependenciesInfo {
    // only keep dependency info block for builds with Firebase
    includeInApk = useFirebase
    includeInBundle = useFirebase
  }

  buildFeatures.buildConfig = true

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = file(checkNotNull(releaseSigning["STORE_FILE"]))
        storePassword = checkNotNull(releaseSigning["STORE_PASSWORD"])
        keyAlias = checkNotNull(releaseSigning["KEY_ALIAS"])
        keyPassword = checkNotNull(releaseSigning["KEY_PASSWORD"])
      }
    }
  }

  flavorDimensions += listOf("pageType")
  productFlavors {
    create("madani") {
      applicationId = "com.moham.qurankareem"
    }
  }

  buildTypes {
    create("beta") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard.cfg")
      signingConfig = signingConfigs.findByName("release")
      versionNameSuffix = "-beta"
      matchingFallbacks += listOf("debug", "release")
    }

    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
      matchingFallbacks += "release"
    }

    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard.cfg")
      signingConfig = signingConfigs.findByName("release")
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        it.testLogging {
          events("passed", "skipped", "failed", "standardOut", "standardError")
          showStandardStreams = true
        }
      }
    }
  }

  packaging {
    resources {
      excludes += setOf("META-INF/*.kotlin_module", "META-INF/DEPENDENCIES", "META-INF/INDEX.LIST")
    }
  }
}

tasks.configureEach {
  val normalizedTaskName = name.lowercase(Locale.US)
  val createsInstallableArtifact =
    normalizedTaskName.startsWith("package") ||
      normalizedTaskName.startsWith("bundle") ||
      normalizedTaskName.startsWith("assemble")
  val isProtectedBuild =
    normalizedTaskName.contains("release") || normalizedTaskName.contains("beta")
  if (createsInstallableArtifact && isProtectedBuild) {
    doFirst {
      check(hasReleaseSigning) {
        "Release signing is required. Provide STORE_FILE, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
      }
    }
  }
}

androidComponents {
  onVariants { variant ->
    val applicationId = variant.applicationId.get()
    val unitTestManifestPlaceholders = (variant as? HasUnitTest)?.unitTest?.manifestPlaceholders
    val oauthRedirectScheme = syncProperty(
      name = "QURAN_OAUTH_REDIRECT_SCHEME",
      applicationId = applicationId,
      variantName = variant.name,
      flavorName = variant.flavorName
    ).ifBlank { applicationId }
    val oauthRedirectHost = "callback"

    variant.resValues.put(variant.makeResValueKey("string", "authority"),
      ResValue("$applicationId.data.QuranDataProvider")
    )
    variant.resValues.put(variant.makeResValueKey("string", "file_authority"),
      ResValue("$applicationId.fileprovider")
    )
    variant.resValues.put(variant.makeResValueKey("string", "quran_sync_oauth_redirect_scheme"),
      ResValue(oauthRedirectScheme)
    )
    variant.resValues.put(variant.makeResValueKey("string", "quran_sync_oauth_client_id"),
      ResValue(syncProperty("QURAN_OAUTH_CLIENT_ID", applicationId, variant.name, variant.flavorName))
    )
    variant.resValues.put(variant.makeResValueKey("string", "quran_sync_oauth_issuer_url"),
      ResValue(syncProperty("QURAN_OAUTH_ISSUER_URL", applicationId, variant.name, variant.flavorName))
    )
    variant.resValues.put(variant.makeResValueKey("string", "quran_sync_environment"),
      ResValue(syncProperty("QURAN_SYNC_ENVIRONMENT", applicationId, variant.name, variant.flavorName))
    )
    // Keep these placeholders aligned with QuranSyncConfig.redirectUri.
    mapOf(
      "oidcRedirectScheme" to oauthRedirectScheme,
      "oidcRedirectHost" to oauthRedirectHost,
      "oidcPostLogoutRedirectScheme" to oauthRedirectScheme,
      "oidcPostLogoutRedirectHost" to oauthRedirectHost
    ).forEach { (name, value) ->
      variant.manifestPlaceholders.put(name, value)
      unitTestManifestPlaceholders?.put(name, value)
    }

    if (applicationId.endsWith("debug")) {
      variant.manifestPlaceholders.put("app_debug_label", "القرآن الكريم")
      unitTestManifestPlaceholders?.put("app_debug_label", "القرآن الكريم")
    }
  }
}

fun syncProperty(
  name: String,
  applicationId: String,
  variantName: String,
  flavorName: String?
): String {
  val suffixes = buildList {
    add(applicationId.toPropertySuffix())
    add(variantName.toPropertySuffix())
    flavorName?.let { add(it.toPropertySuffix()) }
  }

  return suffixes
    .asSequence()
    .mapNotNull { suffix -> oauthProperties.getProperty("${name}_$suffix")?.trim() }
    .firstOrNull()
    ?: oauthProperties.getProperty(name)?.trim()
    ?: ""
}

fun String.toPropertySuffix(): String {
  return replace(Regex("[^A-Za-z0-9]"), "_").uppercase(Locale.US)
}

// required so that Errorprone doesn't look at generated files
afterEvaluate {
  tasks.withType<JavaCompile>().configureEach {
    (options as ExtensionAware).extensions.configure<ErrorProneOptions> {
      excludedPaths.set(".*/build/generated/.*")
      disableWarningsInGeneratedCode = true
    }
  }
}

if (File(rootDir, "extras/extras.gradle").exists()) {
  apply(from = File(rootDir, "extras/extras.gradle"))
} else {
  apply(from = "pluggable.gradle")
}

dependencies {
  implementation(project(":common:analytics"))
  implementation(project(":common:audio"))
  implementation(project(":common:bookmark"))
  implementation(project(":common:data"))
  implementation(project(":common:di"))
  implementation(project(":common:download"))
  implementation(project(":common:networking"))
  implementation(project(":common:pages"))
  implementation(project(":common:preference"))
  implementation(project(":common:reading"))
  implementation(project(":common:recitation"))
  implementation(project(":common:search"))
  implementation(project(":common:toolbar"))
  implementation(project(":common:translation"))
  implementation(project(":common:upgrade"))
  implementation(project(":common:ui:core"))

  implementation(project(":feature:audio"))
  implementation(project(":feature:audiobar"))
  implementation(project(":feature:ayahbookmark"))
  implementation(project(":feature:downloadmanager"))
  implementation(project(":feature:qarilist"))

  // android auto support
  implementation(project(":feature:autoquran"))

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  implementation(libs.retrofit)
  implementation(libs.converter.moshi)

  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.media)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.localbroadcastmanager)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.androidx.recyclerview)
  implementation(libs.material)
  implementation(libs.androidx.swiperefreshlayout)
  implementation(libs.androidx.window)

  // compose
  implementation(libs.compose.foundation)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.ui.tooling)

  // okio
  implementation(libs.okio)

  // rx
  implementation(libs.rxjava)
  implementation(libs.rxandroid)

  // analytics
  debugImplementation(project(":feature:analytics-noop"))
  add("betaImplementation", project(":feature:analytics-noop"))
  if (useFirebase) {
    releaseImplementation(project(":feature:firebase-analytics"))
  } else {
    releaseImplementation(project(":feature:analytics-noop"))
  }

  // workmanager
  implementation(libs.androidx.work.runtime.ktx)

  implementation(libs.okhttp)

  implementation(libs.moshi)
  ksp(libs.moshi.codegen)

  implementation(libs.insetter)
  implementation(libs.timber)
  testImplementation(project(":common:test-utils"))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.okhttp.mockserver)
  testImplementation(libs.junit.ktx)
  testImplementation(libs.robolectric)
  testImplementation(libs.espresso.core)
  testImplementation(libs.espresso.intents)
  testImplementation(libs.turbine)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.sqldelight.sqlite.driver)
  testImplementation(libs.sqldelight.primitive.adapters)

  errorprone(libs.errorprone.core)

  // Number Picker
  implementation(libs.number.picker)

}

// Kover coverage configuration
kover {
  reports {
    filters {
      excludes {
        // Exclude generated code (patterns target DI-generated classes only)
        classes(
          "*_Factory",
          "*_Factory\$*",
          "*_MembersInjector",
          "*_Module",
          "*_Module\$*",
          "*Module_*",
          "*Binding*",
          "*_Impl",
          "*_Impl\$*",
          "*.BuildConfig",
          "*.databinding.*",
          "*.R",
          "*.R\$*",
          "*Hilt*",
          "*_HiltModules*",
          "*_ComponentTreeDeps*",
          "*_Provide*Factory*"
        )
        packages(
          "*.di.*",
          "*.generated.*",
          "dagger.hilt.*",
          "hilt_aggregated_deps.*"
        )
        annotatedBy(
          "dagger.*",
          "javax.inject.*",
          "androidx.compose.ui.tooling.preview.Preview"
        )
      }
    }
  }
}
