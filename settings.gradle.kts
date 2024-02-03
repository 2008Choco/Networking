pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }

        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "networking"

include("networking-common", "networking-bukkit", "networking-fabric")
