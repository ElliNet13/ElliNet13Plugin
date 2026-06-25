import org.gradle.kotlin.dsl.register

plugins {
    java
}

group = "com.ellinet13"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.register("deployMc") {
    dependsOn("build")

    val username = providers.systemProperty("user.name").orElse("unknown")

    val jarFile = layout.buildDirectory.file(
        "libs/ElliNet13Plugin-${project.version}.jar"
    )

    doLast {
        val actualUser = username.get()

        if (actualUser != "ellinet13") {
            println("Not ellinet13 ($actualUser), skipping deploy")
            return@doLast
        }

        val file = jarFile.get().asFile

        if (!file.exists()) {
            println("Jar not found: ${file.absolutePath}")
            return@doLast
        }

        val remote = "ellinet13@100.111.32.11"
        val remotePath = "~/updatedmc/plugins/"

        println("Deleting old plugin jars on server...")

        val deleteProcess = ProcessBuilder(
            "ssh",
            "-o",
            "StrictHostKeyChecking=accept-new",
            remote,
            "rm -f ~/updatedmc/plugins/ElliNet13Plugin-*.jar"
        )
            .inheritIO()
            .start()

        val deleteExit = deleteProcess.waitFor()

        if (deleteExit != 0) {
            throw GradleException("Failed to delete old plugin jars (code $deleteExit)")
        }

        println("Uploading new jar...")

        val uploadProcess = ProcessBuilder(
            "rsync",
            "-avz",
            "-e",
            "ssh -o StrictHostKeyChecking=accept-new",
            file.absolutePath,
            "$remote:$remotePath"
        )
            .inheritIO()
            .start()

        val exitCode = uploadProcess.waitFor()

        if (exitCode == 0) {
            println("Deploy successful!")
        } else {
            throw GradleException("Deploy failed with code $exitCode")
        }
    }
}