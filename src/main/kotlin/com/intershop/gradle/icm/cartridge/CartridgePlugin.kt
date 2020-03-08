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
import com.intershop.gradle.icm.ICMBasePlugin.Companion.CONFIGURATION_CARTRIDGE
import com.intershop.gradle.icm.ICMBasePlugin.Companion.CONFIGURATION_CARTRIDGERUNTIME
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.WriteCartridgeClasspath
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer

/**
 * The base cartridge plugin applies all basic
 * configuration and tasks to a cartridge project.
 */
open class CartridgePlugin : Plugin<Project> {

    companion object {

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

            if (!checkForTask(
                    tasks,
                    CopyThirdpartyLibs.DEFAULT_NAME
                )
            ) {
                tasks.register(
                    CopyThirdpartyLibs.DEFAULT_NAME,
                    CopyThirdpartyLibs::class.java)
            }
        }
    }

    private fun configureAddFileCreation(project: Project) {
        with(project.configurations) {

            val runtime = getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            val cartridge = getByName(CONFIGURATION_CARTRIDGE)
            val cartridgeRuntime = getByName(CONFIGURATION_CARTRIDGERUNTIME)

            val tasksWriteFiles = HashSet<Task>()

            if (!checkForTask(
                    project.tasks,
                    WriteCartridgeDescriptor.DEFAULT_NAME
                )
            ) {
                val taskWriteCartridgeDescriptor = project.tasks.register(
                    WriteCartridgeDescriptor.DEFAULT_NAME, WriteCartridgeDescriptor::class.java
                ) {
                    it.dependsOn(cartridge, cartridgeRuntime)
                }
                tasksWriteFiles.add(taskWriteCartridgeDescriptor.get())
            }

            if (!checkForTask(
                    project.tasks,
                    WriteCartridgeClasspath.DEFAULT_NAME
                )
            ) {
                val taskWriteCartridgeClasspath = project.tasks.register(
                    WriteCartridgeClasspath.DEFAULT_NAME, WriteCartridgeClasspath::class.java
                ) {
                    it.dependsOn(cartridgeRuntime, runtime)
                }
                tasksWriteFiles.add(taskWriteCartridgeClasspath.get())
            }

            project.rootProject.tasks.findByName(ICMBasePlugin.TASK_WRITECARTRIDGEFILES)?.dependsOn(tasksWriteFiles)
        }
    }
}
