rootProject.name = "SemanticEmojiFilter"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Opt-in second stage: the Laya decision model as a verifier behind the embedding filter.
// It needs the SKaiNET-transformers `decisions` and `laya` modules, which ship with 0.59.0;
// until then they come from a local publish (`publishToMavenLocal` as 0.59.0-SNAPSHOT).
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
        if (layaVerifier) {
            mavenLocal { content { includeGroupByRegex("sk\\.ainet(\\..*)?") } }
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
