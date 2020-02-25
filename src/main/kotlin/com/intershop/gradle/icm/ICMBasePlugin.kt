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

package com.intershop.gradle.icm

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.CreateClusterID
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import java.time.Year

/**
 * The base plugin for the configuration of the ICM project.
 */
open class ICMBasePlugin: Plugin<Project> {

    companion object {
        const val TASK_ALLDEPENDENCIESREPORT = "allDependencies"
        const val TASK_WRITECARTRIDGEFILES = "writeCartridgeFiles"

        /**
         * checks if the specified name is available in the list of tasks.
         *
         * @param taskname  the name of the new task
         * @param tasks     the task container self
         */
        fun checkForTask(tasks: TaskContainer, taskname: String): Boolean {
            return tasks.names.contains(taskname)
        }
    }

    override fun apply(project: Project) {
        with(project) {
            if (project.rootProject == this) {

                logger.info("ICM build plugin will be initialized")

                // apply maven publishing plugin to root and subprojects
                plugins.apply(MavenPublishPlugin::class.java)
                subprojects {
                    plugins.apply(MavenPublishPlugin::class.java)
                }

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

                configureClusterIdTask(project)
                configureCreateServerInfoPropertiesTask(project, extension)

                if(! checkForTask(tasks, TASK_ALLDEPENDENCIESREPORT)) {
                    tasks.register(TASK_ALLDEPENDENCIESREPORT, DependencyReportTask::class.java)
                }

                tasks.maybeCreate(TASK_WRITECARTRIDGEFILES).apply {
                    group = "ICM cartridge build"
                    description = "Lifecycle task for ICM cartridge build"
                }

            } else {
                logger.warn("ICM build plugin will be not applied to the sub project '{}'", name)
            }
        }
    }

    private fun configurePublishing(project: Project, extension: IntershopExtension) {
        project.extensions.configure(PublishingExtension::class.java) { publishing ->
            publishing.publications.maybeCreate(
                extension.mavenPublicationName,
                MavenPublication::class.java
            ).apply {
                versionMapping {
                    it.usage("java-api") {
                        it.fromResolutionResult()
                    }
                    it.usage("java-runtime") {
                        it.fromResolutionResult()
                    }
                }

                pom.description.set(project.description)
                pom.inceptionYear.set(Year.now().value.toString())
                pom.properties.set(mapOf("cartridge.name" to project.name))
            }
        }
    }

    private fun configureCreateServerInfoPropertiesTask(project: Project, extension: IntershopExtension) {
        with(project) {
            if(! checkForTask(tasks, CreateServerInfoProperties.DEFAULT_NAME)) {
                tasks.register(
                    CreateServerInfoProperties.DEFAULT_NAME,
                    CreateServerInfoProperties::class.java
                ) { task ->
                    task.provideProductId(extension.projectInfo.productIDProvider)
                    task.provideProductName(extension.projectInfo.productNameProvider)
                    task.provideCopyrightOwner(extension.projectInfo.copyrightOwnerProvider)
                    task.provideCopyrightFrom(extension.projectInfo.copyrightFromProvider)
                    task.provideOrganization(extension.projectInfo.organizationProvider)
                }
            }
        }
    }

    private fun configureClusterIdTask(project: Project) {
        with(project) {
            if (!checkForTask(tasks, CreateClusterID.DEFAULT_NAME)) {
                tasks.register(
                    CreateClusterID.DEFAULT_NAME,
                    CreateClusterID::class.java
                )
            }
        }
    }
}
