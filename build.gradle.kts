plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.13.1"
}

group = "com.mongodb"
version = "0.1.2-SNAPSHOT"


repositories {
    mavenCentral()
}

intellij {
    version.set("2022.2.4")
    type.set("IC")

    plugins.set(listOf("java", "gradle", "JUnit"))
}

val copyAgentJar by tasks.registering(Copy::class) {
    dependsOn(":instrumentation-agent:build")
    from("${rootProject.projectDir}/instrumentation-agent/build/libs")
    include("*.jar")
    into("${projectDir}/src/main/resources")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // Make sure buildPlugin depends on copyAgentJar
    named("buildPlugin") {
        dependsOn(copyAgentJar)
    }
}
