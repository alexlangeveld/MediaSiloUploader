plugins {
    id("java")
}

group = "nl.alexflix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Add additional repositories if needed
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("jakarta.mail:jakarta.mail-api:2.1.3")
    implementation("jakarta.activation:jakarta.activation-api:2.0.1")
    implementation("org.eclipse.angus:jakarta.mail:2.0.0")
    implementation("com.google.code.gson:gson:2.8.9")
//    implementation("org.jline:jline-terminal:3.26.2")
    implementation("org.jline:jline-terminal:3.21.0")
    implementation(("com.googlecode.lanterna:lanterna:3.1.1"))

    implementation("com.amazonaws:aws-java-sdk-s3:1.12.765")
//     https://mvnrepository.com/artifact/software.amazon.awssdk/s3
    implementation("software.amazon.awssdk:s3:2.27.4")

}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = "nl.alexflix.mediasilouploader.Main" //  main class
    }

    // Create a fat JAR with all dependencies
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    archiveFileName.set("MediaSiloUploader.jar")
    archiveVersion.set("1.0")
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}