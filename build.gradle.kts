plugins {
    id("eclipse")
    id("java-library")
    id("maven-publish")
}

subprojects {
    apply(plugin = "checkstyle")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "wtf.choco"
    version = "0.1.2"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }

        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        api("org.jetbrains:annotations:24.1.0")
        implementation("com.google.guava:guava:33.0.0-jre")
    }

    tasks {
        withType<JavaCompile> {
           options.release = 17
           options.encoding = Charsets.UTF_8.name()
        }

        withType<Javadoc> {
            val options = (options as org.gradle.external.javadoc.StandardJavadocDocletOptions)
			options.encoding = Charsets.UTF_8.name()
            options.links("https://docs.oracle.com/en/java/javase/17/docs/api/")
        }

        withType<Checkstyle> {
            configFile = file("${rootDir}/checkstyle.xml")
        }

        withType<Test> {
            useJUnitPlatform()
        }
    }

    publishing {
        repositories {
            maven {
                isAllowInsecureProtocol = true
                val repository = if (project.version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"
                url = uri("http://repo.choco.wtf/$repository")

                credentials {
                    username = project.properties["mavenUsername"].toString()
                    password = project.properties["mavenAccessToken"].toString()
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
