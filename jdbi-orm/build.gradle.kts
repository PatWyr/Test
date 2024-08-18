dependencies {
    // remember this is a Java project :) Kotlin only for tests
    testImplementation(kotlin("stdlib-jdk8"))

    // logging
    implementation(libs.slf4j.api)

    // db
    api(libs.jdbi3)

    // validation support
    api(libs.jakarta.validation)  // to have JSR303 validations in the entities
    testImplementation(libs.bundles.hibernate.validator)

    // tests
    testImplementation(libs.bundles.gson)
    testImplementation(libs.hikaricp)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.h2)

    testImplementation(libs.bundles.lucene) // for H2 Full-Text search
    testImplementation(libs.bundles.jdbc)
    testImplementation(libs.bundles.testcontainers)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Java has no nullable types
    api(libs.jetbrains.annotations)
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("jdbi-orm")
