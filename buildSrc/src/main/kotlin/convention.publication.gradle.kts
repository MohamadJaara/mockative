import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.KotlinMultiplatform
import java.util.Properties

plugins {
    signing
    id("com.vanniktech.maven.publish")
}

val props = Properties().apply {
    project.properties.forEach { (key, value) -> value?.let { this[key] = it } }
    loadFile(project.rootProject.file("local.properties"), required = false)
    loadEnv("signing.keyId", "SIGNING_KEY_ID")
    loadEnv("signing.key", "SIGNING_KEY")
    loadEnv("signing.password", "SIGNING_PASSWORD")
}

fun prop(key: String): String? = props.getProperty(key)?.takeIf { it.isNotBlank() }

val signingKeyId = prop("signing.keyId")
val signingKey = prop("signing.key")
val signingPassword = prop("signing.password")
val signingEnabled = signingKeyId != null && signingKey != null

logger.lifecycle("Signing configuration: keyId=${signingKeyId?.take(4)}..., key=${if (signingKey != null) "${signingKey.length} chars" else "null"}, password=${if (signingPassword != null) "set" else "null"}")
logger.lifecycle("Signing enabled: $signingEnabled")

// Configure signing BEFORE mavenPublishing block
if (signingEnabled) {
    signing {
        val decodedKey = try {
            String(java.util.Base64.getDecoder().decode(signingKey))
        } catch (e: IllegalArgumentException) {
            throw GradleException("Failed to decode signing.key from Base64: ${e.message}")
        }
        useInMemoryPgpKeys(signingKeyId, decodedKey, signingPassword)
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    if (signingEnabled) {
        signAllPublications()
    }

    // Only configure KotlinMultiplatform if the kotlin multiplatform plugin is applied
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        configure(KotlinMultiplatform())
    }

    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString()
    )

    pom {
        name = "Mockative"
        description = "Mocking framework for Kotlin, Kotlin/Native and Kotlin Multiplatform (Fork)"
        inceptionYear = "2021"
        url = "https://github.com/mohamadjaara/mockative"

        licenses {
            license {
                name = "MIT"
                url = "https://github.com/mohamadjaara/mockative/blob/main/LICENSE"
            }
        }

        developers {
            developer {
                id = "mohamadjaara"
                name = "Mohamad Jaara"
                email = "mohamadjaara@users.noreply.github.com"
            }
        }

        scm {
            url = "https://github.com/mohamadjaara/mockative"
            connection = "scm:git:git://github.com/mohamadjaara/mockative.git"
            developerConnection = "scm:git:ssh://github.com/mohamadjaara/mockative.git"
        }
    }
}

if (!signingEnabled) {
    signing { isRequired = false }
    tasks.withType<Sign>().configureEach { enabled = false }
}

publishing {
    repositories {
        maven {
            name = "GitHubRelease"
            url = uri("${project.rootProject.projectDir}/release")
        }
    }
}

// Ensure all publications use the correct group and version
// but exclude plugin marker publications which need their own groupId
afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        // Don't override groupId for plugin marker publications
        // The java-gradle-plugin creates these with groupId matching the plugin ID
        if (!name.contains("PluginMarkerMaven")) {
            groupId = project.group.toString()
        }
        version = project.version.toString()
    }
}