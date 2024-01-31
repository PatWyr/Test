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
    testImplementation(libs.dynatest)
    testImplementation(libs.gson)
    testImplementation(libs.hikaricp)
    // workaround for https://github.com/google/gson/issues/1059
    testImplementation(libs.gsonjavatime)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.h2)

    testImplementation(libs.bundles.lucene) // for H2 Full-Text search
    testImplementation(libs.bundles.jdbc)
    testImplementation(libs.bundles.testcontainers)

    // Java has no nullable types
    api(libs.jetbrains.annotations)
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("jdbi-orm")
