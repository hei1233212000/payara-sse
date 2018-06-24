import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.War

import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.noarg.gradle.NoArgExtension

buildscript {
    val kotlinVersion: String by extra { "1.2.50" }

    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-noarg:$kotlinVersion")
    }
}

plugins {
    kotlin("jvm") version "1.2.50"
    java
    war
    id("org.jetbrains.kotlin.plugin.allopen") version "1.2.50"
    id("org.jetbrains.kotlin.plugin.noarg") version "1.2.50"
}

group = "poc"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

val jeeVersion = "8.0"
val payaraMicroVersion = "5.182"

dependencies {
    compile(kotlin("stdlib-jdk8"))
    providedCompile("javax:javaee-api:$jeeVersion")
    providedCompile("fish.payara.extras:payara-micro:$payaraMicroVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val payaraMicroJarDir = "$buildDir/payara-micro"
val payaraMicroJarName = "payara-micro.jar"
val payaraMicroJarPath = "$payaraMicroJarDir/$payaraMicroJarName"

val warTask = tasks["war"] as War
val explodedWarDir = "$buildDir/${project.name}"

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