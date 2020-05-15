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

package com.intershop.gradle.icm.project

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.extension.ProjectConfiguration
import com.intershop.gradle.icm.tasks.CreateConfigFolder
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateSitesFolder
import com.intershop.gradle.icm.tasks.ExtendCartridgeList
import com.intershop.gradle.icm.tasks.ProvideCartridgeListTemplate
import com.intershop.gradle.icm.tasks.ProvideLibFilter
import com.intershop.gradle.icm.tasks.SetupCartridges
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Tar

class PluginConfig(val project: Project,
                   val projectLayout: ProjectLayout) {

    private val projectConfig : ProjectConfiguration by lazy {
        project.extensions.getByType(IntershopExtension::class.java).projectConfig
    }

    /**
     * Configures the 3rd party copy task from an enumeration.
     *
     * @param taskconf enumeration with all necessary parameters
     * @return Sync task
     */
    fun get3rdPartyCopyTask(taskconf: TaskConfCopyLib): Sync {
        with(project) {
            return tasks.maybeCreate( taskconf.taskname(), Sync::class.java ).apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = taskconf.description()

                this.into(taskconf.targetpath(projectLayout))
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }

    /**
     * Configures the task for the setup of external cartridges.
     *
     * @param libFilterTask task provides a file with libs of the base container project
     * @param type environment type
     * @param environmentTypesList List of environment types
     */
    fun getSetupCartridgesTask(libFilterTask: ProvideLibFilter,
                               type: EnvironmentType,
                               environmentTypesList: List<EnvironmentType> ): SetupCartridges {
        return with(project) {
            tasks.maybeCreate(
                TaskName.valueOf(type.name).cartridges(),
                SetupCartridges::class.java
            ).apply {
                provideCartridges(projectConfig.cartridges)
                provideDBprepareCartridges(projectConfig.dbprepareCartridges)
                provideLibFilterFile(libFilterTask.outputFile)
                environmentTypes.set(environmentTypesList)
                provideOutputDir(TargetConf.valueOf(type.name).cartridges(projectLayout))
            }
        }
    }

    fun getSitesTask(type: EnvironmentType): CreateSitesFolder {
        with(project) {
            return tasks.maybeCreate( TaskName.valueOf(type.name).sites(), CreateSitesFolder::class.java ).apply {
                baseProject.set(projectConfig.base)

                projectConfig.modules.all {
                    module(it)
                }

                baseDirConfig.set(projectConfig.serverDirConfig.base.sites)
                extraDirConfig.set(projectConfig.serverDirConfig.getServerDirSet(type.name).sites)

                provideOutputDir(TargetConf.valueOf(type.name).sites(projectLayout))
            }
        }
    }

    fun getConfigTask(templateTask: ProvideCartridgeListTemplate,
                      versionInfoTask: CreateServerInfo,
                      type: EnvironmentType,
                      environmentTypesList: List<EnvironmentType>): CreateConfigFolder {
        with(project) {

            val task = TaskName.valueOf(type.name)
            val target = TargetConf.valueOf(type.name)

            val cartridgeListTask = tasks.maybeCreate(task.cartridgelist(), ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateTask.outputFile)
                provideCartridges(projectConfig.cartridges)
                provideDBprepareCartridges(projectConfig.dbprepareCartridges)

                environmentTypes.set(environmentTypesList)

                provideOutputFile(target.cartridgelist(projectLayout))

                dependsOn(templateTask)
            }

            return tasks.maybeCreate(task.config(), CreateConfigFolder::class.java).apply {
                baseProject.set(projectConfig.base)

                projectConfig.modules.all {
                    module(it)
                }

                baseDirConfig.set(projectConfig.serverDirConfig.base.config)
                extraDirConfig.set(projectConfig.serverDirConfig.getServerDirSet(type.name).config)

                provideCartridgeListFile(cartridgeListTask.outputFile)
                provideVersionInfoFile(versionInfoTask.outputFile)

                provideOutputDir(target.config(projectLayout))
            }
        }
    }

    fun configureInitTask(sitesFolderTask: CreateSitesFolder, taskname: String) {
        with(project) {

            val pgkTask = tasks.getByName(taskname) as Tar

            pgkTask.with(
                project.copySpec { cp ->
                    cp.from(sitesFolderTask.outputs)
                }
            )
            pgkTask.dependsOn(sitesFolderTask)
        }
    }

    fun configurePackageTask(configTask: CreateConfigFolder,
                             cartridgesTask: SetupCartridges,
                             copyLibs: Sync,
                             taskname: String) {

        with(project) {
            val createPackage = tasks.getByName(taskname) as Tar

            createPackage.with(
                getCopySpecFor(configTask, cartridgesTask, copyLibs)
            )

            createPackage.dependsOn(cartridgesTask)
            createPackage.dependsOn(configTask)
            createPackage.dependsOn(copyLibs)
        }
    }

    fun configurePrepareTask(type: EnvironmentType): Task {
        return project.tasks.maybeCreate(TaskName.valueOf(type.name).prepare()).apply {
            group = IntershopExtension.INTERSHOP_GROUP_NAME
            description = "starts all tasks for the preparation of a '${type}' build dir"
        }
    }

    fun getTask(taskname: String): Task {
        return project.tasks.getByName(taskname)
    }

    private fun getCopySpecFor(configTask: CreateConfigFolder,
                               cartridgesTask: SetupCartridges,
                               copyLibs: Sync): CopySpec {
        return project.copySpec { cp ->
            cp.from(cartridgesTask.outputs) { cps ->
                cps.into("cartridges")
                cps.exclude("libs/**")
            }
            cp.from(cartridgesTask.outputs) { cps ->
                cps.into("libs")
                cps.include("libs/**")
                cps.includeEmptyDirs = false
                cps.eachFile { details ->
                    val targetPath = details.path.replaceFirst("libs/", "")
                    details.path = targetPath
                }
            }
            cp.from(configTask.outputs)
            cp.from(copyLibs.outputs) { cps ->
                cps.into("libs")
            }
        }
    }
}
