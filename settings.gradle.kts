pluginManagement {
    repositories {
        google ()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "CuteStickers"
include(":app")
include(":core:data")
include(":core:model")
include(":core:domain")
include(":core:network")
include(":core:designsystem")
include(":core:ui")
include(":core:common")
include(":core:database")
include(":feature:cats")
include(":feature:dogs")
include(":feature:media-details")
include(":feature:stickers")
include(":feature:my-stickers")
