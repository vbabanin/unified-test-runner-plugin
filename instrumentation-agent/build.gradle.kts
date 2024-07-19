import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.mongodb.agent"
version = "1.0-SNAPSHOT"

java {
    withSourcesJar()
}

/*
    * The shadowJar task is used to create a single JAR file that contains all the dependencies needed
    * for seamless execution of the plugin. Moreover, net.bytebuddy is relocated to another folder so that it is not
    * conflicting with the mongo-java-driver byte-buddy dependencies when plugin is executed.
    * If not relocated, lower byte-buddy version might get picked up from mongo-java-driver and cause issues.
 */
tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(
                /*
                 * Make sure that the main class remains in com.mongodb package. Otherwise, there might be issues with
                 * byte-buddy instrumentation, because some instrumented classes won't have module access to the advices
                 *  generated in Main class.
                 *  Status: To be investigated.
                 */
                "Premain-Class" to "com.mongodb.Main",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true"
            )
        }
        relocate("net.bytebuddy", "my.shaded.bytebuddy")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

repositories {
    mavenCentral()
}

tasks.register("prepareKotlinBuildScriptModel") {}

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.17.1")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("net.bytebuddy:byte-buddy:1.14.11")
    implementation("org.mongodb:mongodb-driver-sync:4.11.0")
    implementation("net.bytebuddy:byte-buddy-agent:1.14.11")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation(platform("org.junit:junit-bom:5.9.1"))
    implementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}