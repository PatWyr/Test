dependencies {
    // remember this is a Java project :) Kotlin only for tests
    testImplementation(kotlin("stdlib-jdk8"))

    // logging
    implementation("org.slf4j:slf4j-api:${properties["slf4jVersion"]}")

    // db
    api("org.jdbi:jdbi3-core:3.42.0")

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
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4jVersion"]}")
    testImplementation("com.h2database:h2:2.2.224") // https://repo1.maven.org/maven2/com/h2database/h2/

    testImplementation("org.postgresql:postgresql:42.7.1") // check newest at https://jdbc.postgresql.org/download/
    testImplementation("com.mysql:mysql-connector-j:8.2.0") // https://dev.mysql.com/downloads/connector/j/
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.3.1") // https://mariadb.com/kb/en/about-mariadb-connector-j/
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11")

    testImplementation("org.testcontainers:testcontainers:${properties["testcontainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${properties["testcontainersVersion"]}")
    testImplementation("org.testcontainers:mysql:${properties["testcontainersVersion"]}")
    testImplementation("org.testcontainers:mariadb:${properties["testcontainersVersion"]}")
    testImplementation("org.testcontainers:mssqlserver:${properties["testcontainersVersion"]}")
    testImplementation("org.testcontainers:cockroachdb:${properties["testcontainersVersion"]}")

    // Java has no nullable types
    api("org.jetbrains:annotations:24.0.1")
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("jdbi-orm")

