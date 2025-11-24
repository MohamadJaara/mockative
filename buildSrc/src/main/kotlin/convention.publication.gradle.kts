import com.vanniktech.maven.publish.SonatypeHost
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
val signingEnabled = signingKeyId != null && signingKey != null

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    if (signingEnabled) {
        signAllPublications()
    }

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

if (signingEnabled) {
    signing {
        val decodedKey = try {
            String(java.util.Base64.getDecoder().decode(signingKey))
        } catch (e: IllegalArgumentException) {
            throw GradleException("Failed to decode signing.key from Base64: ${e.message}")
        }
        useInMemoryPgpKeys(signingKeyId, decodedKey, prop("signing.password"))
        sign(publishing.publications)
    }
} else {
    signing { isRequired = false }
    tasks.withType<Sign>().configureEach { enabled = false }
    logger.lifecycle("Signing disabled: signing.keyId or signing.key not provided")
}

publishing {
    repositories {
        maven {
            name = "GitHubRelease"
            url = uri("${project.rootProject.projectDir}/release")
        }
    }
}