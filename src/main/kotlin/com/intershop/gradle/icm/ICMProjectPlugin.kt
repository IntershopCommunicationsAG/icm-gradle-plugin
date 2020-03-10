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
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import java.io.File
import javax.inject.Inject

/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(private var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {
        const val CONFIGURATION_EXTERNALCARTRIDGES = "extCartridge"

        const val EXT_CARTRIDGELIST_TEST = "extendCartrideListTest"
        const val EXT_CARTRIDGELIST_PROD = "extendCartrideListProd"

        const val PREPARE_PROJECT_CONF = "prepareConfig"
        const val PREPARE_SITES_CONF = "prepareSites"

        const val CREATE_SITES_FOLDER = "createSites"
        const val CREATE_CONF_FOLDER = "createConfig"

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
            val extension = extensions.findByType(
                IntershopExtension::class.java
            ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

            val cartridge = configurations.maybeCreate(CONFIGURATION_EXTERNALCARTRIDGES)
            cartridge.isTransitive = false

            val prepareTask = tasks.maybeCreate("prepareServer").apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "starts all tasks for the preparation of an local server"
            }

            configureProjectPackages(this, extension, prepareTask)
            configureCartridgeListTasks(this, extension, prepareTask)
            configureExtCartridgeTask(this, extension, prepareTask)
            configureSyncTasks(this, extension, prepareTask)

        }
    }

    private fun configureCartridgeListTasks(project: Project, extension: IntershopExtension, prepareTask: Task) {
        with(project) {
            // create task for test cartridge list properties
            tasks.maybeCreate(EXT_CARTRIDGELIST_TEST, ExtendCartridgeList::class.java).apply {
                    provideCartridges(extension.projectConfig.cartridgesProvider)
                    provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                    provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)

                    writeAllCartridgeList = true

                    provideOutputfile(projectLayout.buildDirectory.file("test/${CARTRIDGELISTFILE_NAME}"))

                    val inputProp = project.objects.fileProperty()
                    inputProp.set(
                        File(tasks.getByName(PREPARE_PROJECT_CONF).outputs.files.single(),
                            "system-conf/cluster/${CARTRIDGELISTFILE_NAME}"))
                    provideCartridgePropertiesFile(inputProp)

                    dependsOn(tasks.getByName(PREPARE_PROJECT_CONF))
                    prepareTask.dependsOn(this)
                }

            // create task for production cartridge list properties
            tasks.maybeCreate(EXT_CARTRIDGELIST_PROD, ExtendCartridgeList::class.java).apply {
                    provideCartridges(extension.projectConfig.cartridgesProvider)
                    provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                    provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)

                    writeAllCartridgeList = false

                    provideOutputfile(projectLayout.buildDirectory.file("production/${CARTRIDGELISTFILE_NAME}"))

                    val inputProp = project.objects.fileProperty()
                    inputProp.set(
                        File(tasks.getByName(PREPARE_PROJECT_CONF).outputs.files.single(),
                            "system-conf/cluster/${CARTRIDGELISTFILE_NAME}"))
                    provideCartridgePropertiesFile(inputProp)

                    dependsOn(tasks.getByName(PREPARE_PROJECT_CONF))
                }
        }
    }

    private fun configureProjectPackages(project: Project, extension: IntershopExtension, prepareTask: Task) {
        with(project) {
            tasks.maybeCreate(PREPARE_PROJECT_CONF,
                DownloadPackage::class.java).apply {
                classifier = "configuration"
                provideDependency(extension.projectConfig.configurationPackageProvider)
                provideOutputDir(projectLayout.buildDirectory.dir("org_release/configuration"))
                prepareTask.dependsOn(this)
            }

            tasks.maybeCreate(PREPARE_SITES_CONF,
                DownloadPackage::class.java).apply {
                classifier = "sites"
                provideDependency(extension.projectConfig.sitesPackageProvider)
                provideOutputDir(projectLayout.buildDirectory.dir("org_release/sites"))
                prepareTask.dependsOn(this)
            }
        }
    }

    private fun configureSyncTasks(project: Project, extension: IntershopExtension, prepareTask: Task) {
        with(project) {
            tasks.maybeCreate(CREATE_SITES_FOLDER, Copy::class.java).apply {
                from(extension.projectConfig.sitesDir) {
                    it.into("sites")
                }

                from(tasks.getByName(PREPARE_SITES_CONF)) {
                    it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }

                into(projectLayout.buildDirectory.dir("server/sites"))

                prepareTask.dependsOn(this)
            }

            tasks.maybeCreate(CREATE_CONF_FOLDER, Copy::class.java).apply {
                from(extension.projectConfig.configDir) {
                    exclude("**/**/cartridgelst.properties")
                    it.into("system-conf")
                }

                from(tasks.getByName(PREPARE_PROJECT_CONF)) {
                    exclude("**/**/cartridgelst.properties")

                    it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }

                from(tasks.getByName(EXT_CARTRIDGELIST_TEST)) {
                    it.into("system-conf/cluster")
                }

                into(projectLayout.buildDirectory.dir("server/conf"))

                prepareTask.dependsOn(this)
            }
        }
    }

    private fun configureExtCartridgeTask(project: Project, extension: IntershopExtension, prepareTask: Task) {
        with(project) {
            tasks.maybeCreate(SetupExternalCartridges.DEFAULT_NAME, SetupExternalCartridges::class.java).apply {
                provideCartridgeDir(extension.projectConfig.cartridgeDirProvider)
                prepareTask.dependsOn(this)
            }
        }
    }
}
