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

import com.intershop.gradle.icm.cartridge.CartridgePlugin
import com.intershop.gradle.icm.cartridge.ProductPlugin
import com.intershop.gradle.icm.cartridge.TestPlugin
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.internal.tasks.CreateInitPackage
import com.intershop.gradle.icm.internal.tasks.CreateInitTestPackage
import com.intershop.gradle.icm.internal.tasks.CreateMainPackage
import com.intershop.gradle.icm.internal.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.CreateClusterID
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor.Companion.DEFAULT_NAME
import com.intershop.gradle.isml.IsmlPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.diagnostics.DependencyReportTask

/**
 * The base plugin for the configuration of the ICM project.
 */
open class ICMBasePlugin: Plugin<Project> {

    companion object {
        const val TASK_ALLDEPENDENCIESREPORT = "allDependencies"
        const val TASK_WRITECARTRIDGEFILES = "writeCartridgeFiles"

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
            if (project.rootProject == this) {

                logger.info("ICM build plugin will be initialized")

                // apply maven publishing plugin to root and subprojects
                plugins.apply(MavenPublishPlugin::class.java)

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

                plugins.withType(JavaPlugin::class.java) {
                    configureBaseConfigurations(this)
                }

                subprojects.forEach { prj ->
                    prj.plugins.withType(JavaPlugin::class.java) {
                        configureBaseConfigurations(prj)
                    }
                }

                configureClusterIdTask(this)
                configureCreateServerInfoPropertiesTask(this, extension)

                if(! checkForTask(tasks, TASK_ALLDEPENDENCIESREPORT)) {
                    tasks.register(TASK_ALLDEPENDENCIESREPORT, DependencyReportTask::class.java)
                }

                tasks.maybeCreate(TASK_WRITECARTRIDGEFILES).apply {
                    group = "ICM cartridge build"
                    description = "Lifecycle task for ICM cartridge build"
                }

                createPackageTasks(this)

            } else {
                logger.warn("ICM build plugin will be not applied to the sub project '{}'", name)
            }
        }
    }

    private fun configureBaseConfigurations(project: Project) {
        with(project.configurations) {
            val implementation = findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

            val cartridge = maybeCreate(CONFIGURATION_CARTRIDGE)
            cartridge.isTransitive = false
            implementation?.extendsFrom(cartridge)

            val cartridgeRuntime = maybeCreate(CONFIGURATION_CARTRIDGERUNTIME)
            cartridgeRuntime.extendsFrom(cartridge)
            cartridgeRuntime.isTransitive = true
        }
    }

    private fun configureCreateServerInfoPropertiesTask(project: Project, extension: IntershopExtension) {
        with(project) {
            if(! checkForTask(tasks, CreateServerInfo.DEFAULT_NAME)) {
                tasks.register(
                    CreateServerInfo.DEFAULT_NAME,
                    CreateServerInfo::class.java
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

    private fun createPackageTasks(project: Project) {
        with(project) {
            if(!checkForTask(tasks, CreateInitPackage.DEFAULT_NAME)) {
                tasks.register(CreateInitPackage.DEFAULT_NAME, CreateInitPackage::class.java)
            }

            if(!checkForTask(tasks, CreateInitTestPackage.DEFAULT_NAME)) {
                tasks.register(CreateInitTestPackage.DEFAULT_NAME, CreateInitTestPackage::class.java)
            }

            if(!checkForTask(tasks, CreateMainPackage.DEFAULT_NAME)) {
                tasks.register(CreateMainPackage.DEFAULT_NAME, CreateMainPackage::class.java).get()
            }

            if(!checkForTask(tasks, CreateTestPackage.DEFAULT_NAME)) {
                tasks.register(CreateTestPackage.DEFAULT_NAME, CreateTestPackage::class.java)
            }

            val createMainPackage = tasks.getByName(CreateMainPackage.DEFAULT_NAME) as CreateMainPackage
            val createTestPackage = tasks.getByName(CreateTestPackage.DEFAULT_NAME) as CreateTestPackage

            subprojects {sub ->
                sub.plugins.withType(CartridgePlugin::class.java) {
                    val cartridgefiles = project.copySpec { cp ->
                        cp.from(sub.layout.projectDirectory.dir("staticfiles/cartridge")) { cps ->
                            cps.into("cartridges/${sub.name}/release")
                        }

                        sub.plugins.withType(IsmlPlugin::class.java) {
                            cp.from(sub.tasks.getByName("isml2classMain")) { cpt ->
                               cpt.into{"cartridges/${sub.name}/release"}
                            }
                        }

                        cp.from(sub.tasks.getByName(DEFAULT_NAME)) { cps ->
                            cps.into("cartridges/${sub.name}/release")
                        }

                        sub.plugins.withType(JavaPlugin::class.java) {
                            cp.from(sub.tasks.getByName("jar")) { cps ->
                                cps.into("cartridges/${sub.name}/release/lib")
                            }
                        }
                    }

                    sub.plugins.withType(ProductPlugin::class.java) {
                        sub.plugins.withType(IsmlPlugin::class.java) {
                            createMainPackage.dependsOn(sub.tasks.getByName("isml2classMain"))
                            createMainPackage.dependsOn(sub.tasks.getByName("jar"))
                        }
                        createMainPackage.with(cartridgefiles)
                    }

                    sub.plugins.withType(TestPlugin::class.java) {
                        sub.plugins.withType(IsmlPlugin::class.java) {
                            createTestPackage.dependsOn(sub.tasks.getByName("isml2classMain"))
                            createTestPackage.dependsOn(sub.tasks.getByName("jar"))
                        }
                        createTestPackage.with(cartridgefiles)
                    }
                }
            }
        }
    }
}
