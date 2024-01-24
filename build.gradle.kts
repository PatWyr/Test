import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    java
    signing
}

defaultTasks("clean", "build")

allprojects {
    group = "com.gitlab.mvysny.jdbiorm"
    version = "2.5"

    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("kotlin")
        plugin("org.gradle.signing")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // creates a reusable function which configures proper deployment to Maven Central
    ext["publishing"] = { artifactId: String ->

        java {
            withJavadocJar()
            withSourcesJar()
        }

        tasks.withType<Javadoc> {
            isFailOnError = false
        }

        publishing {
            repositories {
                maven {
                    setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = project.properties["ossrhUsername"] as String? ?: "Unknown user"
                        password = project.properties["ossrhPassword"] as String? ?: "Unknown user"
                    }
                }
            }
            publications {
                create("mavenJava", MavenPublication::class.java).apply {
                    groupId = project.group.toString()
                    this.artifactId = artifactId
                    version = project.version.toString()
                    pom {
                        description = "A very simple persistence framework, built on top of JDBI"
                        name = artifactId
                        url = "https://gitlab.com/mvysny/jdbi-orm"
                        licenses {
                            license {
                                name = "The MIT License"
                                url = "https://opensource.org/licenses/MIT"
                                distribution = "repo"
                            }
                        }
                        developers {
                            developer {
                                id = "mavi"
                                name = "Martin Vysny"
                                email = "martin@vysny.me"
                            }
                        }
                        scm {
                            url = "https://gitlab.com/mvysny/jdbi-orm"
                        }
                    }
                    from(components["java"])
                }
            }
        }

        signing {
            sign(publishing.publications["mavenJava"])
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            // to see the stacktraces of failed tests in the CI console.
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
        }
        systemProperty("h2only", System.getProperty("h2only"))
    }
}

if (JavaVersion.current() > JavaVersion.VERSION_11 && gradle.startParameter.taskNames.contains("publish")) {
    throw GradleException("Release this library with JDK 11 or lower, to ensure JDK11 compatibility; current JDK is ${JavaVersion.current()}")
}

