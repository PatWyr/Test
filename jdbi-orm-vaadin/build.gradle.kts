dependencies {
    implementation("org.slf4j:slf4j-api:${properties["slf4jVersion"]}")
    api(project(":jdbi-orm"))
    api("com.vaadin:flow-data:24.2.4")

    // tests
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
    testImplementation("com.zaxxer:HikariCP:5.0.1")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4jVersion"]}")
    testImplementation("com.h2database:h2:2.2.224") // https://repo1.maven.org/maven2/com/h2database/h2/
    // remember this is a Java project :) Kotlin only for tests
    testImplementation(kotlin("stdlib-jdk8"))
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("jdbi-orm-vaadin")
