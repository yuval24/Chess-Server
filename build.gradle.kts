plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Gson for JSON serialization/deserialization
    implementation("com.google.code.gson:gson:2.10.1")

    // PostgreSQL JDBC driver
    implementation("org.postgresql:postgresql:42.2.24")

    // Auth0 JWT (Java JWT: JSON Web Token for Java and Android)
    implementation("com.auth0:java-jwt:3.18.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}