plugins {
    kotlin("jvm") version "2.0.0"
    id("io.papermc.paperweight.userdev") version "1.7.5"
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("me.champeau.jmh") version "0.7.2"
    id("com.gradleup.shadow") version "8.3.0"
    `maven-publish`
}

group = "net.apogee.slipstream"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    
    implementation(project(":slipstream-generated"))
    
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

jmh {
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
    jmhVersion = "1.37"
    includeTests = true // Позволяет JMH видеть классы из src/main
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        // Tag the normal jar as dev to distinguish it from the shaded production jar
        archiveClassifier.set("dev")
    }

    shadowJar {
        archiveClassifier.set("") // The shaded jar is our main production artifact
        
        mergeServiceFiles() // КРИТИЧЕСКИ ВАЖНО для корутин!

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }

        // Exclude unnecessary metadata
        exclude("META-INF/maven/**")
        exclude("META-INF/proguard/**")
    }
    
    assemble {
        // According to Paperweight docs for MOJANG_PRODUCTION:
        // "you need to remove all dependsOn(reobfJar) lines"
        dependsOn(shadowJar)
    }
}