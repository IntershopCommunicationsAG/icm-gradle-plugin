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
import com.intershop.gradle.icm.tasks.ProvideLibFilter
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.ProjectLayout
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
     * Configures the task for the configuration folder creation.
     *
     * @param versionInfoTask task that creates the server info
     * @param type environment type
     */
    fun getConfigTask(versionInfoTask: TaskProvider<CreateServerInfo>,
                      type: EnvironmentType): TaskProvider<CreateConfigFolder> {
        with(project) {

            val task = TaskName.valueOf(type.name)
            val target = TargetConf.valueOf(type.name)

            return tasks.register(task.config(), CreateConfigFolder::class.java) { cfgTask ->
                cfgTask.baseProject.set(projectConfig.base)

                projectConfig.modules.all {
                    cfgTask.module(it)
                }

                cfgTask.provideVersionInfoFile( project.provider { versionInfoTask.get().outputFile.get() } )

                cfgTask.provideOutputDir(target.config(projectLayout))

                cfgTask.dependsOn(versionInfoTask)
            }
        }
    }

    /**
     * Configures an existing package task package.
     *
     * @param configTask creates the configuration folder>yxc
     * @param taskname specify the task name of the package folder
     */
    fun configurePackageTask(configTask: TaskProvider<CreateConfigFolder>,
                             taskname: String): TaskProvider<Tar> =
        project.tasks.named(taskname, Tar::class.java) { pkg ->
            pkg.with( getCopySpecFor(configTask) )
            pkg.dependsOn(configTask)
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

    private fun getCopySpecFor(configTask: TaskProvider<CreateConfigFolder>): CopySpec =
        project.copySpec { cp ->
            cp.from(configTask.get().outputs)
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
