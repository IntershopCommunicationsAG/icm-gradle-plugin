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
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import com.intershop.gradle.icm.tasks.WriteCartridgeClasspath
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer

/**
 * The base plugin for the configuration of the ICM project.
 */
open class ICMBasePlugin : Plugin<Project> {

    companion object {
        const val CONFIGURATION_CARTRIDGE = "cartridge"
        const val CONFIGURATION_CARTRIDGERUNTIME = "cartridgeRuntime"

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

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(
                    IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java, this
                )

                configureCreateServerInfoPropertiesTask(project, extension)

                val writeCartridgeFiles = tasks.maybeCreate(TASK_WRITECARTRIDGEFILES)

                subprojects.forEach { subProject  ->
                    subProject.plugins.withType(JavaPlugin::class.java) { javaPlugin ->

                        with(subProject.configurations) {
                            val implementation = getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                            val runtime = getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)

                            val cartridge = maybeCreate(CONFIGURATION_CARTRIDGE)
                            cartridge.isTransitive = false
                            implementation.extendsFrom(cartridge)

                            val cartridgeRuntime = maybeCreate(CONFIGURATION_CARTRIDGERUNTIME)
                            cartridgeRuntime.extendsFrom(cartridge)
                            cartridgeRuntime.isTransitive = true

                            if (!checkForTask(tasks, WriteCartridgeDescriptor.DEFAULT_NAME)) {
                                subProject.tasks.register(
                                    WriteCartridgeDescriptor.DEFAULT_NAME,
                                    WriteCartridgeDescriptor::class.java
                                ) {
                                    it.dependsOn(cartridge, cartridgeRuntime)
                                    writeCartridgeFiles.dependsOn(it)
                                }
                            }

                            if (!checkForTask(tasks, WriteCartridgeClasspath.DEFAULT_NAME)) {
                                subProject.tasks.register(
                                    WriteCartridgeClasspath.DEFAULT_NAME,
                                    WriteCartridgeClasspath::class.java
                                ) {
                                    it.dependsOn(cartridgeRuntime)
                                    if (runtime != null) it.dependsOn(runtime)
                                    writeCartridgeFiles.dependsOn(it)
                                }
                            }
                        }

                        if (!checkForTask(subProject.tasks, CopyThirdpartyLibs.DEFAULT_NAME)) {
                            subProject.tasks.register(
                                CopyThirdpartyLibs.DEFAULT_NAME,
                                CopyThirdpartyLibs::class.java
                            )
                        }

                    }
                }
            } else {
                logger.warn("ICM build plugin will be not applied to the sub project '{}'", name)
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
}
