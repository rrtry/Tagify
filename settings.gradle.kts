pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        flatDir {
            dirs("libs")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tagify"
include(":app")
