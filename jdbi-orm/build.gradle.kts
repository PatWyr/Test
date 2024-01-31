dependencies {
    // remember this is a Java project :) Kotlin only for tests
    testImplementation(kotlin("stdlib-jdk8"))

    // logging
    implementation(libs.slf4j.api)

    // db
    api(libs.jdbi3)

    // validation support
    api(libs.jakarta.validation)  // to have JSR303 validations in the entities
    testImplementation(libs.hibernate.validator)
    // EL is required: http://hibernate.org/validator/documentation/getting-started/
    testImplementation(libs.jakarta.el)

    // tests
    testImplementation(libs.dynatest)
    testImplementation(libs.gson)
    testImplementation(libs.hikaricp)
    // workaround for https://github.com/google/gson/issues/1059
    testImplementation(libs.gsonjavatime)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.h2)

    testImplementation(libs.bundles.lucene) // for H2 Full-Text search

    testImplementation("org.postgresql:postgresql:42.7.1") // check newest at https://jdbc.postgresql.org/download/
    testImplementation("com.mysql:mysql-connector-j:8.2.0") // https://dev.mysql.com/downloads/connector/j/
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.3.1") // https://mariadb.com/kb/en/about-mariadb-connector-j/
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11")

    testImplementation(libs.bundles.testcontainers)

    // Java has no nullable types
    api(libs.jetbrains.annotations)
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("jdbi-orm")
