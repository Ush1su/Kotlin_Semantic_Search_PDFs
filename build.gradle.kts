plugins {
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.3.21"
}

group = "org.ai_processor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "veraPdf"
        url = uri("https://artifactory.openpreservation.org/artifactory/vera-dev")

        content {
            includeGroup("org.verapdf")
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.pdfbox:pdfbox:3.0.7")
    implementation("org.opendataloader:opendataloader-pdf-core:2.5.0") {
        exclude(
            group = "com.sun.xml.bind",
            module = "jaxb-core"
        )
    }
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    implementation("io.qdrant:client:1.18.3")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// Frontend
//
// `./gradlew bootJar` builds the React app and packs it into the jar under
// static/, so a single artifact serves the API and the UI from one origin.
//
// Pass -PskipFrontend when frontend/dist has already been built and copied into
// src/main/resources/static — the Docker image does exactly that from a
// separate Node stage, so the JDK stage never needs npm.
// ---------------------------------------------------------------------------

val frontendDirectory = layout.projectDirectory.dir("frontend")
val nodeModulesDirectory = frontendDirectory.dir("node_modules")
val frontendDistDirectory = frontendDirectory.dir("dist")

/**
 * Resolved lazily, and by absolute path: `npm` is often a version-manager
 * symlink that a long-lived Gradle daemon's environment cannot resolve by name.
 * Only tasks that actually build the frontend call this, so a backend-only
 * build never needs Node installed.
 */
fun resolveNpm(): String {
    val names = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("npm.cmd", "npm.exe")
    } else {
        listOf("npm")
    }

    val found = (System.getenv("PATH") ?: "")
        .split(File.pathSeparator)
        .filter { it.isNotBlank() }
        .firstNotNullOfOrNull { directory ->
            names
                .map { File(directory, it) }
                .firstOrNull { it.isFile && it.canExecute() }
        }

    return found?.absolutePath
        ?: error(
            "npm was not found on PATH. Install Node.js 20 or newer, or build with " +
                "-PskipFrontend to produce a jar without the bundled UI."
        )
}

val installFrontend = tasks.register<Exec>("installFrontend") {
    group = "frontend"
    description = "Installs frontend dependencies."

    workingDir = frontendDirectory.asFile
    args("ci")

    inputs.file(frontendDirectory.file("package.json"))
    inputs.file(frontendDirectory.file("package-lock.json"))

    // node_modules is far too large to fingerprint, so track a marker instead
    // and re-run whenever the directory has been removed.
    val marker = layout.buildDirectory.file("frontend-install.marker")
    outputs.file(marker)
    outputs.upToDateWhen { nodeModulesDirectory.asFile.exists() }

    doFirst { executable = resolveNpm() }
    doLast { marker.get().asFile.writeText("installed") }
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    group = "frontend"
    description = "Builds the React app into frontend/dist."

    dependsOn(installFrontend)

    workingDir = frontendDirectory.asFile
    args("run", "build")

    inputs.dir(frontendDirectory.dir("src"))
    inputs.file(frontendDirectory.file("index.html"))
    inputs.file(frontendDirectory.file("vite.config.ts"))
    inputs.file(frontendDirectory.file("package.json"))
    outputs.dir(frontendDistDirectory)

    doFirst { executable = resolveNpm() }
}

// Bundled into the jar rather than into processResources, so `./gradlew test`
// and plain compilation stay independent of Node.
if (!project.hasProperty("skipFrontend")) {
    tasks.bootJar {
        dependsOn(buildFrontend)
        from(frontendDistDirectory) {
            into("BOOT-INF/classes/static")
        }
    }
}
