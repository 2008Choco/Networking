plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
}

repositories {
    maven("https://maven.fabricmc.net")
}

dependencies {
    api(project(":networking-common"))

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}
