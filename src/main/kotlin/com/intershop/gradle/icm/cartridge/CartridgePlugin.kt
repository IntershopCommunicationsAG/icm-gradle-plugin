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
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.WriteCartridgeClasspath
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * The base cartridge plugin applies all basic
 * configuration and tasks to a cartridge project.
 */
open class CartridgePlugin : Plugin<Project> {

    companion object {

        const val CONFIGURATION_CARTRIDGE = "cartridge"
        const val CONFIGURATION_CARTRIDGERUNTIME = "cartridgeRuntime"

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
            with(rootProject) {
                extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(
                    IntershopExtension.INTERSHOP_EXTENSION_NAME,
                    IntershopExtension::class.java
                )
            }
            plugins.apply(JavaPlugin::class.java)

            configureAddFileCreation( this)

            tasks.register( CopyThirdpartyLibs.DEFAULT_NAME, CopyThirdpartyLibs::class.java)
        }
    }

    private fun configureAddFileCreation(project: Project) {
        with(project.configurations) {
            val implementation = getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
            val runtime = getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)

            val cartridge = maybeCreate(CONFIGURATION_CARTRIDGE)
            cartridge.isTransitive = false
            implementation.extendsFrom(cartridge)

            val cartridgeRuntime = maybeCreate(CONFIGURATION_CARTRIDGERUNTIME)
            cartridgeRuntime.extendsFrom(cartridge)
            cartridgeRuntime.isTransitive = true

            val taskWriteCartridgeDescriptor = project.tasks.register(
                WriteCartridgeDescriptor.DEFAULT_NAME,
                WriteCartridgeDescriptor::class.java
                ) {
                    it.dependsOn(cartridge, cartridgeRuntime)
                }

            try {
                project.rootProject.tasks.named(ICMBasePlugin.TASK_WRITECARTRIDGEFILES).configure { task ->
                    task.dependsOn(taskWriteCartridgeDescriptor)
                }
            } catch(ex: UnknownTaskException) {
                project.logger.info("Task {} is not available.", ICMBasePlugin.TASK_WRITECARTRIDGEFILES)
            }

            project.tasks.withType(ProcessResources::class.java) {
                it.dependsOn(taskWriteCartridgeDescriptor)
                it.from(taskWriteCartridgeDescriptor.get().outputFile).into("META-INF/${project.name}")
            }

            if(project.hasProperty("classpath.file.enabled") && project.property("classpath.file.enabled") == "true") {
                if (!checkForTask(project.tasks, WriteCartridgeClasspath.DEFAULT_NAME)) {
                    val taskWriteCartridgeClasspath = project.tasks.register(
                        WriteCartridgeClasspath.DEFAULT_NAME, WriteCartridgeClasspath::class.java
                    ) {
                        it.dependsOn(cartridgeRuntime, runtime)
                    }
                    try {
                        project.rootProject.tasks.named(ICMBasePlugin.TASK_WRITECARTRIDGEFILES).configure { task ->
                            task.dependsOn(taskWriteCartridgeClasspath)
                        }
                    } catch(ex: UnknownTaskException) {
                        project.logger.info("Task {} is not available.", ICMBasePlugin.TASK_WRITECARTRIDGEFILES)
                    }
                }
            }
        }
    }
}
