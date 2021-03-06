import de.marcphilipp.gradle.nexus.NexusPublishExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.Duration

val ossrhUsername: String? = System.getenv("OSSRH_USERNAME")
val ossrhPassword: String? = System.getenv("OSSRH_PASSWORD")
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
val gitCommit = System.getenv("TRAVIS_COMMIT") ?: ""

tasks.wrapper {
    gradleVersion = "6.8.1"
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    id("com.github.ben-manes.versions") version "0.36.0"
    id("de.marcphilipp.nexus-publish") version "0.4.0" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
    idea
}

allprojects {
    group = "ru.bozaro.gitlfs"
    version = "0.18.0-SNAPSHOT"

    apply(plugin = "idea")
    apply(plugin = "com.github.ben-manes.versions")

    repositories {
        mavenCentral()
    }
}

val javaVersion = JavaVersion.VERSION_1_8

idea {
    project.jdkName = javaVersion.name
    project.languageLevel = IdeaLanguageLevel(javaVersion)
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "de.marcphilipp.nexus-publish")

    configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    val api by configurations
    val testImplementation by configurations

    dependencies {
        api("com.google.code.findbugs:jsr305:3.0.2")

        testImplementation("com.google.guava:guava:30.1-jre")
        testImplementation("org.testng:testng:7.3.0")
    }

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true

            // Workaround for https://youtrack.jetbrains.com/issue/IDEA-175172
            outputDir = file("build/classes/main")
            testOutputDir = file("build/classes/test")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useTestNG {
            testLogging {
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = true
            }
        }
    }

    val javadoc by tasks.getting(Javadoc::class) {
        (options as? CoreJavadocOptions)?.addStringOption("Xdoclint:none", "-quiet")
    }

    val javadocJar by tasks.registering(Jar::class) {
        from(javadoc)
        archiveClassifier.set("javadoc")
    }

    val sourcesJar by tasks.registering(Jar::class) {
        val sourceSets: SourceSetContainer by project
        from(sourceSets["main"].allSource)
        archiveClassifier.set("sources")
    }

    configure<NexusPublishExtension> {
        // We're constantly getting socket timeouts on Travis
        connectTimeout.set(Duration.ofMinutes(3))
        clientTimeout.set(Duration.ofMinutes(3))

        repositories {
            sonatype()
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>(project.name) {
                from(components["java"])

                artifact(sourcesJar.get())
                artifact(javadocJar.get())

                pom {
                    name.set(project.name)

                    val pomDescription = description
                    afterEvaluate {
                        pomDescription.set(project.description)
                    }

                    url.set("https://github.com/bozaro/git-lfs-java")

                    scm {
                        connection.set("scm:git:git://github.com/bozaro/git-lfs-java.git")
                        tag.set(gitCommit)
                        url.set("https://github.com/bozaro/git-lfs-java")
                    }

                    licenses {
                        license {
                            name.set("Lesser General Public License, version 3 or greater")
                            url.set("http://www.gnu.org/licenses/lgpl.html")
                        }
                    }

                    developers {
                        developer {
                            id.set("bozaro")
                            name.set("Artem V. Navrotskiy")
                            email.set("bozaro@yandex.ru")
                        }

                        developer {
                            id.set("slonopotamus")
                            name.set("Marat Radchenko")
                            email.set("marat@slonopotamus.org")
                        }
                    }
                }
            }
        }
    }

    val secretKeyRingFile = "${rootProject.projectDir}/secring.gpg"
    extra["signing.secretKeyRingFile"] = secretKeyRingFile
    extra["signing.keyId"] = "4B49488E"
    extra["signing.password"] = signingPassword

    configure<SigningExtension> {
        isRequired = signingPassword != null && file(secretKeyRingFile).exists() && !project.version.toString().endsWith("-SNAPSHOT")

        // TODO: Is it possible to access publishing extension in a safer way?
        val publishing: PublishingExtension by project.extensions
        sign(publishing.publications)
    }
}

tasks.closeRepository.configure {
    onlyIf { !project.version.toString().endsWith("-SNAPSHOT") }
}

tasks.releaseRepository.configure {
    onlyIf { !project.version.toString().endsWith("-SNAPSHOT") }
}

nexusStaging {
    packageGroup = "ru.bozaro"
    stagingProfileId = "365bc6dc8b7aa3"
    username = ossrhUsername
    password = ossrhPassword
}
