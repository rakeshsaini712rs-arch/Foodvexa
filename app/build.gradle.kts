plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.foodvexa.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.foodvexa.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

tasks.named("preBuild").configure {
    doFirst {
        val source = file("src/main/java/com/foodvexa/app/MainActivity.kt")
        var text = source.readText()
        val marker = "card.addView(label(\"• Available\",12f,true,Color.rgb(50,205,120)),margin(0,4,0,0))"
        val badge = "card.addView(label(\"🔥 Fresh & Hot • Near Fast\",11f,true,Color.rgb(255,170,80)),margin(0,3,0,0))"
        if (!text.contains("🔥 Fresh & Hot • Near Fast")) {
            require(text.contains(marker)) { "Product availability line not found" }
            text = text.replace(marker, marker + "\n    " + badge, 1)
            source.writeText(text)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
