plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paper_version")}")
    paperweight.paperDevBundle(rootProject.providers.gradleProperty("paper_version"))
}

val javaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "apiversion" to rootProject.providers.gradleProperty("api_version").get(),
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }

    runServer {
        minecraftVersion(rootProject.providers.gradleProperty("api_version").get())
    }
}