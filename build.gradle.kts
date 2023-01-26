import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.api"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.3.7"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "api.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"


application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-openapi:$vertxVersion")
  implementation("io.vertx:vertx-codegen:$vertxVersion")
  implementation("io.vertx:vertx-jdbc-client:$vertxVersion")
  implementation("io.vertx:vertx-mysql-client:$vertxVersion")
  implementation("com.password4j:password4j:1.6.0")
  implementation("com.mysql:mysql-connector-j:8.0.32")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")

}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--launcher-class=$launcherClassName")
}
