import com.vanniktech.maven.publish.SonatypeHost
import java.util.*

plugins {
    signing
    id("com.vanniktech.maven.publish")
}

val props = Properties().apply {
    // Load `gradle.properties`, environment variables and command-line arguments
    project.properties.forEach { (key, value) ->
        if (value != null) {
            this[key] = value
        }
    }

    // Load `local.properties`
    loadFile(project.rootProject.file("local.properties"), required = false)

    // Load environment variables
    loadEnv("signing.keyId", "SIGNING_KEY_ID")
    loadEnv("signing.key", "SIGNING_KEY")
    loadEnv("signing.password", "SIGNING_PASSWORD")

    loadEnv("sonatype.username", "SONATYPE_USERNAME")
    loadEnv("sonatype.password", "SONATYPE_PASSWORD")
    loadEnv("sonatype.repository", "SONATYPE_REPOSITORY")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    if (props.getProperty("signing.keyId") != null) {
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
                url = "https://github.com/mohamadjaara/mockative/LICENSE"
                distribution = "https://github.com/mohamadjaara/mockative/LICENSE"
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

if (props.getProperty("signing.keyId") != null) {
    signing {
        useInMemoryPgpKeys(
            props.getProperty("signing.keyId"),
            props.getProperty("signing.key"),
            props.getProperty("signing.password"),
        )

        sign(publishing.publications)
    }
}

// Add local release repository
publishing {
    repositories {
        maven {
            name = "GitHubRelease"
            url = uri("${project.rootProject.projectDir}/release")
        }
    }
}
