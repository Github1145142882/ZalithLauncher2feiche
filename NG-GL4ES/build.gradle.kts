plugins {
    id("com.android.library")
}

android {
    namespace = "com.bzlzhh.ng_gl4es"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26
    }

    lint {
        targetSdk = 35
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }

        create("fordebug") {
            initWith(getByName("debug"))
        }
    }

    externalNativeBuild {
        cmake {
            path = file("NG-GL4ES/CMakeLists.txt")
        }
    }

    ndkVersion = "27.0.12077973"
}
