rootProject.name = "SemanticEmojiFilter"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Opt-in second stage: the Laya decision model as a verifier behind the embedding filter
// (the SKaiNET-transformers `decisions` and `laya` modules, 0.59.0+). Desktop and Android only.
val layaVerifier = providers.gradleProperty("layaVerifier").orNull == "true"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Shared SKaiNET design system (theme + components), consumed as an included build like the
// sibling examples.
includeBuild("../skainet-ui")

include(":androidApp")
include(":composeApp")
include(":shared")
include(":cli")
if (layaVerifier) include(":verifier")
