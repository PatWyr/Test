import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4jVersion = "2.0.7"
// https://testcontainers.com/guides/getting-started-with-testcontainers-for-java
val testcontainersVersion = "1.19.3"

plugins {
    kotlin("jvm") version "1.9.21"
    `maven-publish`
    java
    signing
}

defaultTasks("clean", "build")

group = "com.gitlab.mvysny.jdbiorm"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // remember this is a Java project :) Kotlin only for tests
    testImplementation(kotlin("stdlib-jdk8"))

    // logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    // db
    api("org.jdbi:jdbi3-core:3.37.1")

    // validation support
    api("jakarta.validation:jakarta.validation-api:3.0.2")  // to have JSR303 validations in the entities
    testImplementation("org.hibernate.validator:hibernate-validator:8.0.0.Final")
    // EL is required: http://hibernate.org/validator/documentation/getting-started/
    testImplementation("org.glassfish:jakarta.el:4.0.2")

    // tests
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("com.zaxxer:HikariCP:5.0.1")
    // workaround for https://github.com/google/gson/issues/1059
    testImplementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.2")
    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("com.h2database:h2:2.2.224") // https://repo1.maven.org/maven2/com/h2database/h2/

    testImplementation("org.postgresql:postgresql:42.7.1") // check newest at https://jdbc.postgresql.org/download/
    testImplementation("com.mysql:mysql-connector-j:8.2.0") // https://dev.mysql.com/downloads/connector/j/
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.3.1") // https://mariadb.com/kb/en/about-mariadb-connector-j/
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11")

    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:mysql:$testcontainersVersion")
    testImplementation("org.testcontainers:mariadb:$testcontainersVersion")
    testImplementation("org.testcontainers:mssqlserver:$testcontainersVersion")
    testImplementation("org.testcontainers:cockroachdb:$testcontainersVersion")

    // Java has no nullable types
    api("org.jetbrains:annotations:24.0.1")
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
    systemProperty("h2only", System.getProperty("h2only"))
}
