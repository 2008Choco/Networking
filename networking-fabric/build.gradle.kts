plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven("https://maven.fabricmc.net")
}

dependencies {
    api(project(":networking-common"))
    shadow(project(":networking-common"))

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

    shadowJar {
        configurations = listOf(project.configurations["shadow"])
        exclude("META-INF")

        dependencies {
            include(project(":networking-common"))
        }
    }

    remapJar {
        dependsOn("shadowJar")
        mustRunAfter("shadowJar")
        inputFile.set(shadowJar.get().archiveFile)
    }
}
