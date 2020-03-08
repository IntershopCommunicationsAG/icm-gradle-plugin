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
import com.intershop.gradle.icm.tasks.DownloadPackage
import com.intershop.gradle.icm.tasks.ExtendCartridgeList
import com.intershop.gradle.icm.tasks.ExtendCartridgeList.Companion.CARTRIDGELISTFILE_NAME
import com.intershop.gradle.icm.tasks.SetupExternalCartridges
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskContainer
import javax.inject.Inject

/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {
        const val CONFIGURATION_EXTERNALCARTRIDGES = "extCartridge"

        const val EXT_CARTRIDGELIST_TEST = "extendCartrideListTest"
        const val EXT_CARTRIDGELIST_PROD = "extendCartrideListProd"

        const val PREPARE_PROJECT_CONF = "prepareConfig"
        const val PREPARE_SITES_CONF = "prepareSites"

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
        with(project.rootProject) {
            // add docker plugin to project
            plugins.apply(com.bmuschko.gradle.docker.DockerRemoteApiPlugin::class.java)

            val extension = extensions.findByType(
                IntershopExtension::class.java
            ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

            val cartridge = configurations.maybeCreate(CONFIGURATION_EXTERNALCARTRIDGES)
            cartridge.isTransitive = false

            configureExtCartridgeTask(this, extension)
            configureProjectPackages(this, extension)
            configureCartridgeListTasks(this, extension)
        }
    }

    private fun configureCartridgeListTasks(project: Project, extension: IntershopExtension) {
        with(project) {
            // create task for test cartridge list properties
            if(! checkForTask(tasks, EXT_CARTRIDGELIST_TEST)) {
                tasks.register(
                    EXT_CARTRIDGELIST_TEST,
                    ExtendCartridgeList::class.java
                ) { task ->
                    task.provideCartridges(extension.projectConfig.cartridgesProvider)
                    task.provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                    task.provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)

                    task.writeAllCartridgeList = true

                    task.provideOutputfile(projectLayout.buildDirectory.file("test/${CARTRIDGELISTFILE_NAME}"))
                    task.provideCartridgePropertiesFile(projectLayout.buildDirectory.file("input/${CARTRIDGELISTFILE_NAME}"))
                }
            }
            // create task for production cartridge list properties
            if(! checkForTask(tasks, EXT_CARTRIDGELIST_PROD)) {
                tasks.register(
                    EXT_CARTRIDGELIST_PROD,
                    ExtendCartridgeList::class.java
                ) { task ->
                    task.provideCartridges(extension.projectConfig.cartridgesProvider)
                    task.provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                    task.provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)

                    task.writeAllCartridgeList = false

                    task.provideOutputfile(projectLayout.buildDirectory.file("production/${CARTRIDGELISTFILE_NAME}"))
                    task.provideCartridgePropertiesFile(projectLayout.buildDirectory.file("input/${CARTRIDGELISTFILE_NAME}"))
                }
            }

            // task for syncing configuration - developer configuration

            // task for syncing configuration - production configuration
        }
    }

    private fun configureProjectPackages(project: Project, extension: IntershopExtension) {
        with(project) {
            val prepareFolders = tasks. maybeCreate("prepareFolders")

            tasks.maybeCreate(PREPARE_PROJECT_CONF,
                DownloadPackage::class.java).apply {
                classifier = "configuration"
                provideDependency(extension.projectConfig.configurationPackageProvider)
                provideOutputDir(projectLayout.buildDirectory.dir("org_release/configuration"))
                prepareFolders.dependsOn(this)
            }

            tasks.maybeCreate(PREPARE_SITES_CONF,
                DownloadPackage::class.java).apply {
                classifier = "sites"
                provideDependency(extension.projectConfig.sitesPackageProvider)
                provideOutputDir(projectLayout.buildDirectory.dir("org_release/sites"))
                prepareFolders.dependsOn(this)
            }
        }
    }

    private fun configureExtCartridgeTask(project: Project, extension: IntershopExtension) {
        with(project) {
            if(! checkForTask(tasks, SetupExternalCartridges.DEFAULT_NAME)) {
                tasks.register(
                    SetupExternalCartridges.DEFAULT_NAME,
                    SetupExternalCartridges::class.java
                ) { task ->
                    task.provideCartridgeDir(extension.projectConfig.cartridgeDirProvider)
                }
            }
        }
    }
}
