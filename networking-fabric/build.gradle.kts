plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
    id("com.gradleup.shadow") version "9.2.0"
}

repositories {
    maven("https://maven.fabricmc.net")
}

dependencies {
    api(project(":networking-common"))
    shadow(project(":networking-common"))

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<JavaCompile> {
       options.release = 25
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

    jar {
        dependsOn("shadowJar")
        mustRunAfter("shadowJar")
    }
}
