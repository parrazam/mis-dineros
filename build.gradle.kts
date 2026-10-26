// AGP 9 integra Kotlin (built-in Kotlin) y arrastra su propia versión de KGP en tiempo de
// ejecución. Fijarla aquí a `libs.versions.kotlin` mantiene alineados el compilador, los
// plugins compose/serialization y KSP; sin esto ganaría la versión que traiga AGP.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
