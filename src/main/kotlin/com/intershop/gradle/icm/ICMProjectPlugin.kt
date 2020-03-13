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
import com.intershop.gradle.icm.tasks.CreateConfFolder
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import com.intershop.gradle.icm.tasks.CreateSitesFolder
import com.intershop.gradle.icm.tasks.SetupCartridges
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskContainer
import javax.inject.Inject

/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(private var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {
        const val CONFIGURATION_EXTERNALCARTRIDGES = "extCartridge"

        const val CREATE_SITES_FOLDER = "createSites"
        const val CREATE_CONF_FOLDER = "createConfig"

        const val CREATE_DEVSITES_FOLDER = "createDevSites"
        const val CREATE_DEVCONF_FOLDER = "createDevConfig"

        const val SETUP_CARTRIDGES = "setupCartridges"
        const val SETUP_DEVCARTRIDGES = "setupDevCartridges"

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
                description = "starts all tasks for the preparation of a local server"
            }

            val prepareContainerTask = tasks.maybeCreate("prepareContainer").apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "starts all tasks for the preparation of a container build"
            }

            val confCS: CopySpec = project.copySpec()
            confCS.from(project.layout.projectDirectory.dir("config/base"))
            confCS.from(project.layout.projectDirectory.dir("config/dev"))

            val sitesCS: CopySpec = project.copySpec()
            sitesCS.from(project.layout.projectDirectory.dir("sites/base"))
            sitesCS.from(project.layout.projectDirectory.dir("sites/dev"))

            extension.projectConfig.confCopySpecProperty.convention(confCS)
            extension.projectConfig.sitesCopySpecProperty.convention(sitesCS)

            configureProjectPackages(this, extension, prepareTask, prepareContainerTask)
            configureExtCartridgeTask(this, extension, prepareTask, prepareContainerTask)

        }
    }

    private fun configureProjectPackages(project: Project,
                                         extension: IntershopExtension,
                                         prepareTask: Task,
                                         prepareContainerTask: Task) {
        with(project) {
            val infoTask = tasks.maybeCreate(
                CreateServerInfoProperties.DEFAULT_NAME,
                CreateServerInfoProperties::class.java
            ).apply {
                this.provideProductId(extension.projectInfo.productIDProvider)
                this.provideProductName(extension.projectInfo.productNameProvider)
                this.provideCopyrightOwner(extension.projectInfo.copyrightOwnerProvider)
                this.provideCopyrightFrom(extension.projectInfo.copyrightFromProvider)
                this.provideOrganization(extension.projectInfo.organizationProvider)
            }

            tasks.maybeCreate(CREATE_DEVCONF_FOLDER, CreateConfFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.provideBaseCopySpec(extension.projectConfig.confCopySpecProvider)
                this.provideDevCopySpec(extension.projectConfig.confCopySpecProvider)
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("server/configuration"))

                this.provideCartridges(extension.projectConfig.cartridgesProvider)
                this.provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                this.provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)

                this.writeDevConf = true
                this.provideVersionInfoFile(infoTask.outputFileProperty)
                prepareTask.dependsOn(this)
                this.dependsOn(infoTask)
            }

            tasks.maybeCreate(CREATE_DEVSITES_FOLDER, CreateSitesFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.provideBaseCopySpec(extension.projectConfig.sitesCopySpecProvider)
                this.provideDevCopySpec(extension.projectConfig.sitesCopySpecProvider)
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("server/sites"))

                prepareTask.dependsOn(this)
            }

            tasks.maybeCreate(CREATE_CONF_FOLDER, CreateConfFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.provideBaseCopySpec(extension.projectConfig.confCopySpecProvider)
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("container/configuration"))

                this.provideCartridges(extension.projectConfig.cartridgesProvider)
                this.provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                this.provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)

                this.writeDevConf = false
                this.provideVersionInfoFile(infoTask.outputFileProperty)
                prepareContainerTask.dependsOn(this)
                this.dependsOn(infoTask)
            }

            tasks.maybeCreate(CREATE_SITES_FOLDER, CreateSitesFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.provideBaseCopySpec(extension.projectConfig.sitesCopySpecProvider)
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("container/sites"))

                prepareContainerTask.dependsOn(this)
            }
        }
    }

    private fun configureExtCartridgeTask(project: Project,
                                          extension: IntershopExtension,
                                          prepareTask: Task,
                                          prepareContainerTask: Task) {
        with(project) {
            tasks.maybeCreate(SETUP_DEVCARTRIDGES, SetupCartridges::class.java).apply {
                provideCartridgeDir(extension.projectConfig.cartridgeDirProvider)
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("server/cartridges"))
                prepareTask.dependsOn(this)
            }

            tasks.maybeCreate(SETUP_CARTRIDGES, SetupCartridges::class.java).apply {
                provideCartridgeDir(extension.projectConfig.cartridgeDirProvider)
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.provideProductionCartridges(extension.projectConfig.productionCartridgesProvider)
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("container/cartridges"))

                prepareContainerTask.dependsOn(this)
            }
        }
    }
}

