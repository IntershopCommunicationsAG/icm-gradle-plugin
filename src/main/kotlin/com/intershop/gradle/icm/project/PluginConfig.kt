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

import com.intershop.gradle.icm.ICMProjectPlugin
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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Tar

/**
 * This class provides methods and configurations
 * tasks for the project plugin.
 *
 * @param project   project of project plugin
 * @param projectLayout project layout service
 */
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
    fun get3rdPartyCopyTask(taskconf: TaskConfCopyLib): TaskProvider<Sync> =
        project.tasks.register( taskconf.taskname(), Sync::class.java ) { sync ->
            sync.group = IntershopExtension.INTERSHOP_GROUP_NAME
            sync.description = taskconf.description()

            sync.into(taskconf.targetpath(projectLayout))
            sync.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

    /**
     * Configures the task for the setup of external cartridges.
     *
     * @param type environment type
     * @param environmentTypesList List of environment types
     */
    fun getSetupCartridgesTask(type: EnvironmentType,
                               environmentTypesList: List<EnvironmentType> ): TaskProvider<SetupCartridges> {
        val plTask = project.tasks.named(ICMProjectPlugin.PROVIDE_LIBFILTER, ProvideLibFilter::class.java)

        return project.tasks.register(TaskName.valueOf(type.name).cartridges(), SetupCartridges::class.java) { task ->
            task.provideCartridges(projectConfig.cartridges)
            task.provideDBprepareCartridges(projectConfig.dbprepareCartridges)
            task.provideLibFilterFile( project.provider { plTask.get().outputFile.get() } )

            task.platformDependencies(projectConfig.base.platforms)

            projectConfig.modules.all {
                task.platformDependencies(it.platforms)
            }

            task.environmentTypes.set(environmentTypesList)
            task.provideOutputDir(TargetConf.valueOf(type.name).cartridges(projectLayout))
            task.dependsOn(plTask)
        }
    }

    /**
     * Configures the task for the sites folder creation.
     *
     * @param type environment type
     */
    fun getSitesTask(type: EnvironmentType): TaskProvider<CreateSitesFolder> =
        project.tasks.register( TaskName.valueOf(type.name).sites(), CreateSitesFolder::class.java ) { task ->
            task.baseProject.set(projectConfig.base)

            projectConfig.modules.all {
                task.module(it)
            }

            task.baseDirConfig.set(projectConfig.serverDirConfig.base.sites)
            task.extraDirConfig.set(projectConfig.serverDirConfig.getServerDirSet(type).sites)

            task.provideOutputDir(TargetConf.valueOf(type.name).sites(projectLayout))
        }

    /**
     * Configures the task for the configuration folder creation.
     *
     * @param versionInfoTask task that creates the server info
     * @param type environment type
     * @param environmentTypesList list of environment types for the configuration
     */
    fun getConfigTask(versionInfoTask: TaskProvider<CreateServerInfo>,
                      type: EnvironmentType,
                      environmentTypesList: List<EnvironmentType>): TaskProvider<CreateConfigFolder> {
        with(project) {

            val task = TaskName.valueOf(type.name)
            val target = TargetConf.valueOf(type.name)

            val cartridgeListTemplate = tasks.named(
                ICMProjectPlugin.PROVIDE_CARTRIDGELIST_TEMPLATE, ProvideCartridgeListTemplate::class.java)

            val cartridgeListTask = tasks.register(task.cartridgelist(), ExtendCartridgeList::class.java) { cltask ->
                cltask.provideTemplateFile( project.provider { cartridgeListTemplate.get().outputFile.get() } )
                cltask.provideCartridges(projectConfig.cartridges)
                cltask.provideDBprepareCartridges(projectConfig.dbprepareCartridges)

                cltask.environmentTypes.set(environmentTypesList)

                cltask.provideOutputFile(target.cartridgelist(projectLayout))

                cltask.dependsOn(cartridgeListTemplate)
            }

            return tasks.register(task.config(), CreateConfigFolder::class.java) { cfgTask ->
                cfgTask.baseProject.set(projectConfig.base)

                projectConfig.modules.all {
                    cfgTask.module(it)
                }

                cfgTask.baseDirConfig.set(projectConfig.serverDirConfig.base.config)
                cfgTask.extraDirConfig.set(projectConfig.serverDirConfig.getServerDirSet(type).config)

                cfgTask.provideCartridgeListFile( project.provider { cartridgeListTask.get().outputFile.get() } )
                cfgTask.provideVersionInfoFile( project.provider { versionInfoTask.get().outputFile.get() } )

                cfgTask.provideOutputDir(target.config(projectLayout))

                cfgTask.dependsOn(cartridgeListTask, versionInfoTask)
            }
        }
    }

    /**
     * Configures an existing package task - initalization package.
     *
     * @param sitesFolderTask creates the sites folder
     * @param taskname specify the task name of the package folder
     */
    fun configureInitTask(sitesFolderTask: TaskProvider<CreateSitesFolder>, taskname: String): TaskProvider<Tar> =
        project.tasks.named(taskname, Tar::class.java) { pkg ->
            pkg.with(
                project.copySpec { cp -> cp.from(project.provider { sitesFolderTask.get().outputs }) }
            )
            pkg.dependsOn(sitesFolderTask)
        }

    /**
     * Configures an existing package task package.
     *
     * @param configTask creates the configuration folder>yxc
     * @param cartridgesTask creates the folder with external cartridges
     * @param copyLibs copy 3rd party libs
     * @param taskname specify the task name of the package folder
     */
    fun configurePackageTask(configTask: TaskProvider<CreateConfigFolder>,
                             cartridgesTask: TaskProvider<SetupCartridges>,
                             copyLibs: TaskProvider<Sync>,
                             taskname: String): TaskProvider<Tar> =
        project.tasks.named(taskname, Tar::class.java) { pkg ->
            pkg.with( getCopySpecFor(configTask, cartridgesTask, copyLibs) )
            pkg.dependsOn(cartridgesTask, configTask, copyLibs)
        }

    /**
     * Configures the main task for the preparation.
     *
     * @param type environment type
     */
    fun configurePrepareTask(type: EnvironmentType): TaskProvider<Task> =
        project.tasks.register(TaskName.valueOf(type.name).prepare()) { task ->
            task.group = IntershopExtension.INTERSHOP_GROUP_NAME
            task.description = "starts all tasks for the preparation of a '${type}' build dir"
        }

    private fun getCopySpecFor(configTask: TaskProvider<CreateConfigFolder>,
                               cartridgesTask: TaskProvider<SetupCartridges>,
                               copyLibs: TaskProvider<Sync>): CopySpec =
        project.copySpec { cp ->
            cp.from( project.provider { cartridgesTask.get().outputs } ) { cps ->
                cps.into("cartridges")
                cps.exclude("libs/**")
                cps.exclude("**/**/.git*")
            }
            cp.from( project.provider { cartridgesTask.get().outputs } ) { cps ->
                cps.into("lib")
                cps.include("libs/**")
                cps.exclude("**/**/.git*")
                cps.includeEmptyDirs = false
                cps.eachFile { details ->
                    val targetPath = details.path.replaceFirst("libs/", "")
                    details.path = targetPath
                }
            }
            cp.from(configTask.get().outputs)
            cp.from( project.provider { copyLibs.get().outputs } ) { cps ->
                cps.into("lib")
            }
        }

    /**
     * Configures task for the preparation of
     * the cartridge list properties template.
     */
    fun getCartridgeListTemplate(): TaskProvider<ProvideCartridgeListTemplate> =
        project.tasks.register(
            ICMProjectPlugin.PROVIDE_CARTRIDGELIST_TEMPLATE,
            ProvideCartridgeListTemplate::class.java
        ) { task ->
            task.provideBaseDependency(projectConfig.base.dependency)
            task.provideFileDependency(projectConfig.cartridgeListDependency)
        }

    /**
     * Get lib filter file task.
     */
    fun createLibFilterFile() =
        project.tasks.register( ICMProjectPlugin.PROVIDE_LIBFILTER, ProvideLibFilter::class.java ) {task ->
            task.provideBaseDependency(projectConfig.base.dependency)
            task.provideFileDependency(projectConfig.libFilterFileDependency)
        }
}
