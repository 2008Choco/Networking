# Networking

Born from [VeinMiner (Bukkit)](https://github.com/2008Choco/VeinMiner/)'s networking code to facilitate communication between a Bukkit server plugin and Fabric client mod, Networking aims to provide a platform-independent framework for modded Minecraft client-server communication. With an object-friendly API in mind, developers are able to create a protocol that may be shared between client and server codebases without having to copy and paste the same packet contents.

While Networking does support any platform, its usefulness shines through especially in projects that do not share a common library. For instance, a Bukkit plugin wanting to communicate with a Fabric mod. Or a Sponge server wanting to communicate with a Forge client. Networking may be used in a modded environment with shared libraries, but often their in-built APIs for client-server communication tend to be sufficient and more accustomed to their own types.

## Dependency Information

Currently supported platforms are `bukkit`, and `fabric`. If your platform is not listed, you may use `common` as a platform, you will just need to implement the `ChannelRegistrar` implementation for yourself. Or [contribute it here](https://github.com/2008Choco/Networking/pulls)! Pull requests are welcomed.

Support for Forge and NeoForge are planned. Sponge is not planned, but feel free to contribute.

**Maven:**
```xml
<repositories>
    <repository>
        <id>choco-repo</id>
        <url>https://repo.choco.wtf/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>wtf.choco</groupId>
        <artifactId>networking-{PLATFORM}</artifactId>
        <version>{VERSION}</version>
    </dependency>
</dependencies>
```

**Gradle (Groovy):**
```groovy
repositories {
    maven { url = "https://repo.choco.wtf/releases" }
}

dependencies {
    implementation 'wtf.choco:networking-{PLATFORM}:{VERSION}'
}
```

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    maven { url = "https://repo.choco.wtf/releases" }
}

dependencies {
    implementation("wtf.choco:networking-{PLATFORM}:{VERSION}")
}
```

## Usage/Examples

For usage information and examples on how to use Networking, please refer to [the wiki](https://github.com/2008Choco/Networking/wiki) which will go more in-depth about how to create a protocol, defining packets (messages), protocol configurations, and ChannelRegistrars for supported platforms.

## Used By

If you would like to see this library in action, [VeinMiner (Bukkit)](https://github.com/2008Choco/Networking) uses it heavily to communicate with its client-sided mod.
