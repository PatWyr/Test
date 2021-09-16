import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4jVersion = "1.7.32"
val testcontainersVersion = "1.16.0"

plugins {
    kotlin("jvm") version "1.5.21"
    `maven-publish`
    java
    signing
}

defaultTasks("clean", "build")

group = "com.gitlab.mvysny.jdbiorm"
version = "0.8-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // remember this is a Java project :) Kotlin only for tests
    testImplementation(kotlin("stdlib-jdk8"))

    // logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    // db
    api("org.jdbi:jdbi3-core:3.18.0")
    testImplementation("com.zaxxer:HikariCP:4.0.3")

    // validation support
    api("javax.validation:validation-api:2.0.1.Final")  // to have JSR303 validations in the entities
    testImplementation("org.hibernate.validator:hibernate-validator:6.2.0.Final")
    // EL is required: http://hibernate.org/validator/documentation/getting-started/
    testImplementation("org.glassfish:javax.el:3.0.1-b08")

    // tests
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:0.20")
    testImplementation("com.google.code.gson:gson:2.8.7")
    // workaround for https://github.com/google/gson/issues/1059
    testImplementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.1")
    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("com.h2database:h2:1.4.200")

    testImplementation("org.postgresql:postgresql:42.2.5")
    testImplementation("mysql:mysql-connector-java:5.1.48")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.4.0")
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:8.4.1.jre8")

    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:mysql:$testcontainersVersion")
    testImplementation("org.testcontainers:mariadb:$testcontainersVersion")
    testImplementation("org.testcontainers:mssqlserver:$testcontainersVersion")

    // Java has no nullable types
    api("org.jetbrains:annotations:21.0.1")
}

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
            this.artifactId = "jdbi-orm"
            version = project.version.toString()
            pom {
                description.set("A very simple persistence framework, built on top of JDBI")
                name.set("JDBI-ORM")
                url.set("https://gitlab.com/mvysny/jdbi-orm")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("mavi")
                        name.set("Martin Vysny")
                        email.set("martin@vysny.me")
                    }
                }
                scm {
                    url.set("https://gitlab.com/mvysny/jdbi-orm")
                }
            }
            from(components["java"])
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in Travis-CI console.
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
