repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    api(project(":networking-common"))
    api("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
}
