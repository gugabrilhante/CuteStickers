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
    }
}

rootProject.name = "CuteCats"
include(":app")
include(":core:data")
include(":core:model")
include(":core:domain")
include(":core:network")
include(":core:designsystem")
include(":core:ui")
include(":core:common")
include(":feature:cats")
include(":feature:dogs")
include(":feature:media-details")
include(":feature:stickers")
