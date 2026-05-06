import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Lee git en tiempo de configuración usando providers.exec, que es la API soportada
// por el configuration cache de Gradle. ignoreExitValue=true evita que falle la build
// si git no está disponible o no hay historial; comprobamos el exitValue antes de
// considerar válida la salida.
fun gitOutput(vararg args: String): Provider<String> = providers.exec {
    commandLine("git", *args)
    isIgnoreExitValue = true
}.let { result ->
    result.result.zip(result.standardOutput.asText) { execResult, output ->
        if (execResult.exitValue == 0) output.trim() else ""
    }
}

val gitVersionName: String = gitOutput("describe", "--tags", "--always")
    .map { it.removePrefix("v").ifEmpty { "dev" } }
    .getOrElse("dev")

val gitVersionCode: Int = gitOutput("rev-list", "--count", "HEAD")
    .map { it.toIntOrNull() ?: 1 }
    .getOrElse(1)

println("[mis-dineros] versionCode=$gitVersionCode versionName=$gitVersionName")

android {
    namespace = "com.parra.misdineros"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.parra.misdineros"
        minSdk = 26
        targetSdk = 35
        versionCode = gitVersionCode
        versionName = gitVersionName

        testInstrumentationRunner = "com.parra.misdineros.HiltTestRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore/release.jks")
            if (keystoreFile.exists()) {
                val storePass = System.getenv("KEYSTORE_PASSWORD") ?: ""
                // KEY_PASSWORD puede diferir de KEYSTORE_PASSWORD; si no se define,
                // se asume que ambas son iguales (caso más común con keytool por defecto).
                val keyPass = System.getenv("KEY_PASSWORD") ?: storePass
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = System.getenv("KEY_ALIAS") ?: "mis-dineros"
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // Core library desugaring (for java.time on API < 26 if needed, and LocalDate support)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navegación
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.workmanager.runtime.ktx)

    // Serialización
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Gráficos
    implementation(libs.vico.compose.m3)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.workmanager.testing)
    androidTestImplementation(libs.hilt.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.android.compiler)
}
