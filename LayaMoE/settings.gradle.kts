rootProject.name = "LayaMoE"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
        // Local-dev lane: `-PskainetMavenLocal` resolves sk.ainet artifacts from ~/.m2 (an unsigned
        // `publishToMavenLocal` of the SKaiNET or SKaiNET-transformers repo) ahead of Maven Central —
        // how an unreleased engine or transformers version is tried here before it is published.
        if (providers.gradleProperty("skainetMavenLocal").isPresent) {
            mavenLocal { content { includeGroupByRegex("""sk\.ainet.*""") } }
        }
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
include(":webWorker")
