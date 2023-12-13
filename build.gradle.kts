import org.asciidoctor.gradle.jvm.AsciidoctorTask

/*
 * Copyright 2021 Intershop Communications AG.
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

    kotlin("jvm") version "1.9.21"

    // test coverage
    jacoco

    // ide plugin
    idea

    // publish plugin
    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing

    `jvm-test-suite`

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    // documentation
    id("org.jetbrains.dokka") version "1.9.10"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.intershop.gradle.icm"
description = "Intershop Commerce Management Plugins"
// apply gradle property 'projectVersion' to project.version, default to 'LOCAL'
val projectVersion : String? by project
version = projectVersion ?: "LOCAL"

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

val pluginUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
val pluginTags = listOf("intershop", "build", "icm")
gradlePlugin {
    website = pluginUrl
    vcsUrl = pluginUrl
    plugins {
        create("icmBasePlugin") {
            id = "com.intershop.gradle.icm.base"
            implementationClass = "com.intershop.gradle.icm.ICMBasePlugin"
            displayName = "icm-base-plugin"
            description = "This ICM plugin contains configuration and some main aspects of all plugins."
            tags = pluginTags
        }
        create("icmProjectPlugin") {
            id = "com.intershop.gradle.icm.project"
            implementationClass = "com.intershop.gradle.icm.ICMProjectPlugin"
            displayName = "icm-project-plugin"
            description = "This plugin should be applied to Intershop Commerce Management customer projects."
            tags = pluginTags
        }
        create("icmCartridgePlugin") {
            id = "com.intershop.icm.cartridge"
            implementationClass = "com.intershop.gradle.icm.cartridge.CartridgePlugin"
            displayName = "icm-cartridge"
            description = "The cartridge plugin applies all basic configurations and tasks."
            tags = pluginTags
        }
        create("icmContainerPlugin") {
            id = "com.intershop.icm.cartridge.container"
            implementationClass = "com.intershop.gradle.icm.cartridge.ContainerPlugin"
            displayName = "icm-container-cartridge"
            description = "The container cartridge plugin applies all basic configurations and tasks for container cartridges."
            tags = pluginTags
        }
        create("icmProductCartridgePlugin") {
            id = "com.intershop.icm.cartridge.product"
            implementationClass = "com.intershop.gradle.icm.cartridge.ProductPlugin"
            displayName = "icm-product-cartridge"
            description = "The product cartridge plugin applies all basic configurations and tasks for product cartridges."
            tags = pluginTags
        }
        create("icmTestCartridgePlugin") {
            id = "com.intershop.icm.cartridge.test"
            implementationClass = "com.intershop.gradle.icm.cartridge.TestPlugin"
            displayName = "icm-test-cartridge"
            description = "The test cartridge plugin applies all basic configurations and tasks of an integration test cartridge."
            tags = pluginTags
        }
        create("icmDevelopmentCartridgePlugin") {
            id = "com.intershop.icm.cartridge.development"
            implementationClass = "com.intershop.gradle.icm.cartridge.DevelopmentPlugin"
            displayName = "icm-development-cartridge"
            description = "The development cartridge plugin applies all basic configurations and tasks of and development cartridge."
            tags = pluginTags
        }
        create("icmAdapterCartridgePlugin") {
            id = "com.intershop.icm.cartridge.adapter"
            implementationClass = "com.intershop.gradle.icm.cartridge.AdapterPlugin"
            displayName = "icm-adapter-cartridge"
            description = "The adapter cartridge plugin applies all basic configurations and tasks of an external adapter cartridge."
            tags = pluginTags
        }
        create("icmExternalCartridgePlugin") {
            id = "com.intershop.icm.cartridge.external"
            implementationClass = "com.intershop.gradle.icm.cartridge.ExternalPlugin"
            displayName = "icm-external-cartridge"
            description = "The cartridge plugin applies all basic configurations and tasks of an external cartridge (with static folder)."
            tags = pluginTags
        }
        create("icmPublicCartridgePlugin") {
            id = "com.intershop.icm.cartridge.public"
            implementationClass = "com.intershop.gradle.icm.cartridge.PublicPlugin"
            displayName = "icm-external-cartridge"
            description = "The cartridge plugin applies all basic configurations and tasks of an public cartridge (published to Maven)."
            tags = pluginTags
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot"
}

testing {
    suites.withType<JvmTestSuite> {
        useSpock()
        dependencies {
            implementation("com.intershop.gradle.test:test-gradle-plugin:5.0.1")
            implementation(gradleTestKit())
        }

        targets {
            all {
                testTask.configure {
                    systemProperty("intershop.gradle.versions", "8.5")
                }
            }
        }
    }
}

tasks {

    register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = project.layout.buildDirectory.dir("tmp/asciidoctorSrc")
        val inputFiles = fileTree(rootDir) {
            include("**/*.asciidoc")
            exclude("build/**")
        }

        inputs.files.plus( inputFiles )
        outputs.dir( outputDir )

        doFirst {
            outputDir.get().asFile.mkdir()
        }

        from(inputFiles)
        into(outputDir)
    }

    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        setSourceDir(outputDir)
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
            xml.required.set(true)
            html.required.set(true)

            html.outputLocation.set( project.layout.buildDirectory.dir("jacocoHtml") )
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn(test)
    }

    jar.configure {
        dependsOn(asciidoctor)
    }

    dokkaJavadoc.configure {
        outputDirectory.set(project.layout.buildDirectory.dir("dokka"))
    }

    withType<Sign> {
        val sign = this
        withType<PublishToMavenLocal> {
            this.dependsOn(sign)
        }
        withType<PublishToMavenRepository> {
            this.dependsOn(sign)
        }
    }

    afterEvaluate {
        named<Jar>("javadocJar") {
            dependsOn(dokkaJavadoc)
            from(dokkaJavadoc)
        }
    }
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])

            artifact(project.layout.buildDirectory.file("docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(project.layout.buildDirectory.file("docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set(pluginUrl)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                organization {
                    name.set("Intershop Communications AG")
                    url.set("http://intershop.com")
                }
                developers {
                    developer {
                        id.set("m-raab")
                        name.set("M. Raab")
                        email.set("mraab@intershop.de")
                    }
                }
                scm {
                    connection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    developerConnection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    url.set(pluginUrl)
                }
            }
        }
    }
    repositories {

        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    compileOnly("org.apache.ant:ant:1.10.14")
    implementation("com.intershop.version:semantic-version:1.0.0")
}
