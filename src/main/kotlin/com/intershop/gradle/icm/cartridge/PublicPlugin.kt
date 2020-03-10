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
package com.intershop.gradle.icm.cartridge

import com.intershop.gradle.icm.ICMBasePlugin
import com.intershop.gradle.icm.extension.IntershopExtension
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import java.time.Year

/**
 * The cartridge plugin applies all basic configurations
 * for publishing of cartridges.
 */
open class PublicPlugin : Plugin<Project> {

    companion object {
        /**
         * Property for task name to create a source jar.
         */
        const val TASK_SOURCEJAR = "sourcesJar"
        /**
         * Property for task name to create a javadoc jar.
         */
        const val TASK_JAVADOCJAR = "javadocJar"
    }

    override fun apply(project: Project) {
        with(project) {
            plugins.apply(CartridgePlugin::class.java)
            configureAddJars(this)
        }
    }

    private fun configureAddJars(project: Project) {
        with(project) {
            plugins.withType(JavaPlugin::class.java) {
                if (!ICMBasePlugin.checkForTask(
                        tasks,
                        TASK_SOURCEJAR
                    )
                ) {
                    val javaConvention = convention.getPlugin(JavaPluginConvention::class.java)
                    val mainSourceSet = javaConvention.sourceSets.getByName("main")

                    tasks.register(TASK_SOURCEJAR, Jar::class.java) {
                        it.dependsOn(tasks.getByName("classes"))
                        it.archiveClassifier.set("sources")
                        it.from(mainSourceSet.allSource)
                    }
                }

                if (!ICMBasePlugin.checkForTask(
                        tasks,
                        TASK_JAVADOCJAR
                    )
                ) {
                    tasks.register(TASK_JAVADOCJAR, Jar::class.java) {
                        it.dependsOn(tasks.getByName(JavaPlugin.JAVADOC_TASK_NAME))
                        it.archiveClassifier.set("javadoc")
                        it.from(tasks.getByName(JavaPlugin.JAVADOC_TASK_NAME))
                    }
                }

                val extension = rootProject.extensions.getByType(IntershopExtension::class.java)

                plugins.withType(MavenPublishPlugin::class.java) {
                    extensions.configure(PublishingExtension::class.java) { publishing ->
                        publishing.publications.maybeCreate(
                            extension.mavenPublicationName,
                            MavenPublication::class.java
                        ).apply {
                            versionMapping { vm ->
                                vm.usage("java-api") { ms ->
                                    ms.fromResolutionResult()
                                }
                                vm.usage("java-runtime") { ms ->
                                    ms.fromResolutionResult()
                                }
                            }

                            try {
                                from(project.components.getAt("java"))
                            } catch(ex: InvalidUserDataException) {
                                project.logger.warn("Component Java was added to the publication in an other step.")
                            }

                            artifact(tasks.getByName(TASK_SOURCEJAR))
                            artifact(tasks.getByName(TASK_JAVADOCJAR))

                            pom.description.set(project.description)
                            pom.inceptionYear.set(Year.now().value.toString())
                            pom.properties.set(mapOf("cartridge.name" to project.name))
                        }
                    }
                }
            }
        }
    }
}
