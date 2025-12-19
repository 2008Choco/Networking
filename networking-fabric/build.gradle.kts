plugins {
    id("fabric-loom") version "1.13-SNAPSHOT"
    id("com.gradleup.shadow") version "9.2.0"
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<JavaCompile> {
       options.release = 21
       options.encoding = Charsets.UTF_8.name()
    }

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
