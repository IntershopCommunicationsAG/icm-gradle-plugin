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

import com.intershop.gradle.icm.project.PluginConfig
import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import com.intershop.gradle.icm.utils.CartridgeStyle.ALL
import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType.DEVELOPMENT
import com.intershop.gradle.icm.utils.EnvironmentType.PRODUCTION
import com.intershop.gradle.icm.utils.EnvironmentType.TEST
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject


/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(private var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {
        const val PROVIDE_LIBFILTER = "provideLibFilter"

        val PROD_ENVS = listOf(PRODUCTION)
        val TEST_ENVS = listOf(PRODUCTION, TEST)
        val TEST_ONLY_ENVS = listOf(TEST)
        val DEVELOPMENT_ENVS = listOf(PRODUCTION, DEVELOPMENT, TEST)
    }

    override fun apply(project: Project) {
        with(project.rootProject) {
            plugins.apply(ICMBasePlugin::class.java)

            val pluginConfig = PluginConfig(this, projectLayout)

            pluginConfig.createLibFilterFile()
            configureProjectTasks(this, pluginConfig)
        }
    }

    private fun configureProjectTasks(project: Project, pluginConfig: PluginConfig) {
        val infoTask = project.tasks.named(CreateServerInfo.DEFAULT_NAME, CreateServerInfo::class.java)
        val collectLibrariesTask = project.tasks.named("collectLibraries")

        val prepareContainer = prepareContainer(pluginConfig, infoTask).
            apply { configure { task -> task.dependsOn(collectLibrariesTask) } }
        val prepareTestContainer = prepareTestContainer(pluginConfig, infoTask).
            apply { configure { task -> task.dependsOn(collectLibrariesTask) } }
        val prepareServer = prepareServer(pluginConfig, infoTask).
            apply { configure { task -> task.dependsOn(collectLibrariesTask) } }

        configurePrepareTasks(pluginConfig, prepareServer, prepareTestContainer, prepareContainer)
    }

    private fun configurePrepareTasks(pluginConfig: PluginConfig,
                                      prepareServer: TaskProvider<Task>,
                                      prepareTestContainer: TaskProvider<Task>,
                                      prepareContainer: TaskProvider<Task>) {
        pluginConfig.project.subprojects { sub ->
            sub.tasks.withType(WriteCartridgeDescriptor::class.java) { wcdTask ->
                val style = CartridgeUtil.getCartridgeStyle(sub)

                if (style == ALL || DEVELOPMENT_ENVS.contains(style.environmentType())) {
                    prepareServer.configure { task -> task.dependsOn(wcdTask) }
                }
                if (style == ALL || TEST_ONLY_ENVS.contains(style.environmentType())) {
                    prepareTestContainer.configure { task -> task.dependsOn(wcdTask) }
                }
                if (style == ALL || PROD_ENVS.contains(style.environmentType())) {
                    prepareContainer.configure { task -> task.dependsOn(wcdTask) }
                }
            }
        }
    }

    private fun prepareContainer(pluginConfig: PluginConfig,
                                 versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {

        val createConfigProd = pluginConfig.getConfigTask(versionInfoTask, PRODUCTION)

        pluginConfig.configurePackageTask(
            createConfigProd, CreateMainPackage.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(PRODUCTION)
        prepareTask.configure { task ->
            task.dependsOn(createConfigProd)
        }
        return prepareTask
    }

    private fun prepareTestContainer(pluginConfig: PluginConfig,
                                     versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {

        val createConfigTest = pluginConfig.getConfigTask(versionInfoTask, TEST)

        pluginConfig.configurePackageTask(
            createConfigTest, CreateTestPackage.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(TEST)
        prepareTask.configure { task ->
            task.dependsOn(createConfigTest)
        }
        return prepareTask
    }

    private fun prepareServer(pluginConfig: PluginConfig,
                                      versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {
        val createConfig = pluginConfig.getConfigTask(versionInfoTask, DEVELOPMENT)

        val prepareTask = pluginConfig.configurePrepareTask(DEVELOPMENT)
        prepareTask.configure { task ->
            task.dependsOn(createConfig)
        }
        return prepareTask
    }

}
