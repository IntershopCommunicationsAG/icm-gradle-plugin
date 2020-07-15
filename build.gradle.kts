
import com.jfrog.bintray.gradle.BintrayExtension
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
/*
 * Copyright 2019 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {

    // project plugins
    `java-gradle-plugin`
    groovy
    id("nebula.kotlin") version "1.3.50"

    // test coverage
    jacoco

    // ide plugin
    idea

    // publish plugin
    `maven-publish`

    // intershop version plugin
    id("com.intershop.gradle.scmversion") version "6.0.0"

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "2.3.0"

    // documentation
    id("org.jetbrains.dokka") version "0.10.0"

    // code analysis for kotlin
    id("io.gitlab.arturbosch.detekt") version "1.1.1"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "0.11.0"

    // plugin for publishing to jcenter
    id("com.jfrog.bintray") version "1.8.4"
}

scm {
    version.initialVersion = "1.0.0"
}

group = "com.intershop.gradle.icm"
description = "Intershop Commerce Management Plugins"
version = scm.version.version

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("icmBasePlugin") {
            id = "com.intershop.gradle.icm.base"
            implementationClass = "com.intershop.gradle.icm.ICMBasePlugin"
            displayName = "icm-base-plugin"
            description = "This ICM plugin contains configuration and some main aspects of all plugins."
        }
        create("icmProjectPlugin") {
            id = "com.intershop.gradle.icm.project"
            implementationClass = "com.intershop.gradle.icm.ICMProjectPlugin"
            displayName = "icm-project-plugin"
            description = "This plugin should be applied to Intershop Commerce Management customer projects."
        }
        create("icmCartridgePlugin") {
            id = "com.intershop.icm.cartridge"
            implementationClass = "com.intershop.gradle.icm.cartridge.CartridgePlugin"
            displayName = "icm-cartridge"
            description = "The cartridge plugin applies all basic configurations and tasks."
        }
        create("icmContainerPlugin") {
            id = "com.intershop.icm.cartridge.container"
            implementationClass = "com.intershop.gradle.icm.cartridge.ContainerPlugin"
            displayName = "icm-container-cartridge"
            description = "The container cartridge plugin applies all basic configurations and tasks for container cartridges."
        }
        create("icmProductCartridgePlugin") {
            id = "com.intershop.icm.cartridge.product"
            implementationClass = "com.intershop.gradle.icm.cartridge.ProductPlugin"
            displayName = "icm-product-cartridge"
            description = "The product cartridge plugin applies all basic configurations and tasks for product cartridges."
        }
        create("icmTestCartridgePlugin") {
            id = "com.intershop.icm.cartridge.test"
            implementationClass = "com.intershop.gradle.icm.cartridge.TestPlugin"
            displayName = "icm-test-cartridge"
            description = "The test cartridge plugin applies all basic configurations and tasks of an integration test cartridge."
        }
        create("icmDevelopmentCartridgePlugin") {
            id = "com.intershop.icm.cartridge.development"
            implementationClass = "com.intershop.gradle.icm.cartridge.DevelopmentPlugin"
            displayName = "icm-development-cartridge"
            description = "The development cartridge plugin applies all basic configurations and tasks of and development cartridge."
        }
        create("icmAdapterCartridgePlugin") {
            id = "com.intershop.icm.cartridge.adapter"
            implementationClass = "com.intershop.gradle.icm.cartridge.AdapterPlugin"
            displayName = "icm-adpater-cartridge"
            description = "The adpater cartridge plugin applies all basic configurations and tasks of an external adpater cartridge."
        }
        create("icmExternalCartridgePlugin") {
            id = "com.intershop.icm.cartridge.external"
            implementationClass = "com.intershop.gradle.icm.cartridge.ExternalPlugin"
            displayName = "icm-external-cartridge"
            description = "The cartridge plugin applies all basic configurations and tasks of an external cartridge (with static folder)."
        }
        create("icmPublicCartridgePlugin") {
            id = "com.intershop.icm.cartridge.public"
            implementationClass = "com.intershop.gradle.icm.cartridge.PublicPlugin"
            displayName = "icm-external-cartridge"
            description = "The cartridge plugin applies all basic configurations and tasks of an public cartridge (published to Maven)."
        }
    }
}

pluginBundle {
    val pluginURL = "https://github.com/IntershopCommunicationsAG/${project.name}"
    website = pluginURL
    vcsUrl = pluginURL
    tags = listOf("intershop", "gradle", "plugin", "build", "icm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

detekt {
    input = files("src/main/kotlin")
    config = files("detekt.yml")
}

tasks {
    withType<Test>().configureEach {
        systemProperty("intershop.gradle.versions", "6.5")

        dependsOn("jar")
    }

    val copyAsciiDoc = register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = file("$buildDir/tmp/asciidoctorSrc")
        val inputFiles = fileTree(rootDir) {
            include("**/*.asciidoc")
            exclude("build/**")
        }

        inputs.files.plus( inputFiles )
        outputs.dir( outputDir )

        doFirst {
            outputDir.mkdir()
        }

        from(inputFiles)
        into(outputDir)
    }

    withType<AsciidoctorTask> {
        dependsOn(copyAsciiDoc)

        setSourceDir(file("$buildDir/tmp/asciidoctorSrc"))
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        outputOptions {
            setBackends(listOf("html5", "docbook"))
        }

        options = mapOf( "doctype" to "article",
            "ruby"    to "erubis")
        attributes = mapOf(
            "latestRevision"        to  project.version,
            "toc"                   to "left",
            "toclevels"             to "2",
            "source-highlighter"    to "coderay",
            "icons"                 to "font",
            "setanchors"            to "true",
            "idprefix"              to "asciidoc",
            "idseparator"           to "-",
            "docinfo1"              to "true")
    }

    withType<JacocoReport> {
        reports {
            xml.isEnabled = true
            html.isEnabled = true

            html.destination = File(project.buildDir, "jacocoHtml")
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    getByName("bintrayUpload").dependsOn("asciidoctor")
    getByName("jar").dependsOn("asciidoctor")

    val compileKotlin by getting(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
    }

    val dokka by existing(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"

        // Java 8 is only version supported both by Oracle/OpenJDK and Dokka itself
        // https://github.com/Kotlin/dokka/issues/294
        enabled = JavaVersion.current().isJava8
    }

    register<Jar>("sourceJar") {
        description = "Creates a JAR that contains the source code."

        from(sourceSets.getByName("main").allSource)
        archiveClassifier.set("sources")
    }

    register<Jar>("javaDoc") {
        dependsOn(dokka)
        from(dokka)
        archiveClassifier.set("javadoc")
    }
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])
            artifact(tasks.getByName("sourceJar"))
            artifact(tasks.getByName("javaDoc"))

            artifact(File(buildDir, "docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom.withXml {
                val root = asNode()
                root.appendNode("name", project.name)
                root.appendNode("description", project.description)
                root.appendNode("url", "https://github.com/IntershopCommunicationsAG/${project.name}")

                val scm = root.appendNode("scm")
                scm.appendNode("url", "https://github.com/IntershopCommunicationsAG/${project.name}")
                scm.appendNode("connection", "git@github.com:IntershopCommunicationsAG/${project.name}.git")

                val org = root.appendNode("organization")
                org.appendNode("name", "Intershop Communications")
                org.appendNode("url", "http://intershop.com")

                val license = root.appendNode("licenses").appendNode("license")
                license.appendNode("name", "Apache License, Version 2.0")
                license.appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0")
                license.appendNode("distribution", "repo")
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    setPublications("intershopMvn")

    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = project.name
        userOrg = "intershopcommunicationsag"

        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"

        desc = project.description
        websiteUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        issueTrackerUrl = "https://github.com/IntershopCommunicationsAG/${project.name}/issues"

        setLabels("intershop", "gradle", "plugin", "build", "icm")
        publicDownloadNumbers = true

        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = "${project.description} ${project.version}"
            released  = Date().toString()
            vcsTag = project.version.toString()
        })
    })
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    compileOnly("org.apache.ant:ant:1.10.7")
    implementation("com.intershop.gradle.isml:isml-gradle-plugin:3.1.0")

    testImplementation("com.intershop.gradle.test:test-gradle-plugin:3.7.0")
    testImplementation(gradleTestKit())
}

repositories {
    jcenter()
}