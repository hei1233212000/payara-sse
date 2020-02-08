import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.noarg.gradle.NoArgExtension

plugins {
    val kotlinVersion = "1.3.50"
    idea
    kotlin("jvm") version kotlinVersion
    war
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
}

group = "poc"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases-local/")
    }
}

val jakartaeeVersion = "8.0.0"
val payaraMicroVersion = "5.194"

val kluentVersion = "1.49"
val junitVersion = "5.5.0-RC1"
val arquillianVersion = "1.4.1.Final"
val arquillianPayaraMicroContainerVersion = "1.0.Beta3"
val shrinkwrapVersion = "3.1.3"
val restAssuredVersion = "4.0.0"
val jerseyVersion = "2.30"
val gradleToolApiVersion = "5.5.1"

dependencyManagement {
    imports {
        mavenBom("org.jboss.arquillian:arquillian-bom:$arquillianVersion")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    providedCompile("jakarta.platform:jakarta.jakartaee-api:$jakartaeeVersion")
    providedCompile("fish.payara.extras:payara-micro:$payaraMicroVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.junit.vintage:junit-vintage-engine:$junitVersion")
    testImplementation("org.jboss.arquillian.junit:arquillian-junit-container")
    testImplementation("org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-gradle:$shrinkwrapVersion") {
        exclude(module = "gradle-tooling-api")
    }
    testImplementation("org.gradle:gradle-tooling-api:$gradleToolApiVersion")
    testRuntimeOnly("fish.payara.arquillian:arquillian-payara-micro-5-managed:$arquillianPayaraMicroContainerVersion")
    testRuntime("fish.payara.extras:payara-micro:$payaraMicroVersion")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion") {
        // suspend the warning of "'dependencyManagement.dependencies.dependency.systemPath' for com.sun:tools:jar must specify an absolute path but is ${tools.jar} in com.sun.xml.bind:jaxb-osgi:2.2.10"
        exclude(module = "jaxb-osgi")
    }
    testImplementation("org.glassfish.jersey.core:jersey-client:$jerseyVersion")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion")
    testImplementation("org.glassfish.jersey.media:jersey-media-sse:$jerseyVersion")

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val payaraMicroJarDir = "$buildDir/payara-micro"
val payaraMicroJarName = "payara-micro.jar"
val payaraMicroJarPath = "$payaraMicroJarDir/$payaraMicroJarName"

val warTask = tasks["war"] as War
val explodedWarDir = "$buildDir/${project.name}"

tasks.withType<Test> {
    dependsOn("copyPayaraMicro")
    environment("MICRO_JAR", "$payaraMicroJarDir/$payaraMicroJarName")

    useJUnitPlatform {
        includeEngines("junit-vintage")
    }
}

task<Copy>("copyPayaraMicro") {
    from(configurations.providedCompile.files { it.name == "payara-micro" })
    into(payaraMicroJarDir)
    rename { payaraMicroJarName }
}

task<Copy>("explodedWar") {
    into(explodedWarDir)
    with(warTask)
}

task<Exec>("runApp") {
    executable("java")

    args(listOf(
            "-jar",
            payaraMicroJarPath,
            "--autoBindHttp",
            "--nocluster",
            "--deploy",
            explodedWarDir
    ))
}.dependsOn("copyPayaraMicro", "explodedWar")


configure<AllOpenExtension> {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
}

configure<NoArgExtension> {
    annotation("javax.enterprise.context.ApplicationScoped")
}