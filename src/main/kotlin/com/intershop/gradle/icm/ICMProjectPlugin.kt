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
import com.intershop.gradle.icm.extension.ServerDir
import com.intershop.gradle.icm.project.PluginConfig
import com.intershop.gradle.icm.project.TaskName
import com.intershop.gradle.icm.tasks.CollectLibraries
import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.PreparePublishDir
import com.intershop.gradle.icm.utils.CartridgeStyle.ALL
import com.intershop.gradle.icm.utils.CartridgeStyle.valueOf
import com.intershop.gradle.icm.utils.EnvironmentType.DEVELOPMENT
import com.intershop.gradle.icm.utils.EnvironmentType.PRODUCTION
import com.intershop.gradle.icm.utils.EnvironmentType.TEST
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject


/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(private var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {
        const val CARTRIDGELIST_FILENAME = "cartridgelist.properties"
        const val PROVIDE_CARTRIDGELIST_TEMPLATE = "provideCartridgeListTemplate"

        const val PROVIDE_LIBFILTER = "provideLibFilter"

        val PROD_ENVS = listOf(PRODUCTION)
        val TEST_ENVS = listOf(PRODUCTION, TEST)
        val TEST_ONLY_ENVS = listOf(TEST)
        val DEVELOPMENT_ENVS = listOf(PRODUCTION, DEVELOPMENT, TEST)
    }

    override fun apply(project: Project) {
        with(project.rootProject) {
            plugins.apply(ICMBasePlugin::class.java)

            val extension = extensions.findByType(
                IntershopExtension::class.java
            ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

            val pluginConfig = PluginConfig(this, projectLayout)

            pluginConfig.createLibFilterFile()
            configureProjectTasks(this, pluginConfig)

            if(extension.projectConfig.newBaseProject.get()) {
                configureBasePublishingTasks(this, extension)
            } else {
                configureAdapterPublishingTasks(this, extension)
            }
        }
    }

    private fun configureProjectTasks(project: Project, pluginConfig: PluginConfig) {
        val infoTask = project.tasks.named(CreateServerInfo.DEFAULT_NAME, CreateServerInfo::class.java)
        val collectLibrariesTask = project.tasks.named(CollectLibraries.DEFAULT_NAME, CollectLibraries::class.java)
        pluginConfig.getCartridgeListTemplate()

        val prepareContainer = prepareContainer(pluginConfig, infoTask).
            apply { configure { task -> task.dependsOn(collectLibrariesTask) } }
        val prepareTestContainer = prepareTestContainer(pluginConfig, infoTask).
            apply { configure { task -> task.dependsOn(collectLibrariesTask) } }
        val prepareServer = prepareServer(project, pluginConfig, infoTask).
            apply { configure { task -> task.dependsOn(collectLibrariesTask) } }

        configurePrepareTasks(pluginConfig, prepareServer, prepareTestContainer, prepareContainer)
    }

    private fun configurePrepareTasks(pluginConfig: PluginConfig,
                                      prepareServer: TaskProvider<Task>,
                                      prepareTestContainer: TaskProvider<Task>,
                                      prepareContainer: TaskProvider<Task>) {
        pluginConfig.project.subprojects { sub ->
            sub.tasks.withType(Jar::class.java) { jarTask ->
                val styleValue =
                    with(sub.extensions.extraProperties) {
                        if (has("cartridge.style")) {
                            get("cartridge.style").toString()
                        } else {
                            "all"
                        }
                    }
                val style = valueOf(styleValue.toUpperCase())

                if (style == ALL || DEVELOPMENT_ENVS.contains(style.environmentType())) {
                    prepareServer.configure { task -> task.dependsOn(jarTask) }
                }
                if (style == ALL || TEST_ONLY_ENVS.contains(style.environmentType())) {
                    prepareTestContainer.configure { task -> task.dependsOn(jarTask) }
                }
                if (style == ALL || PROD_ENVS.contains(style.environmentType())) {
                    prepareContainer.configure { task -> task.dependsOn(jarTask) }
                }
            }
        }
    }

    private fun prepareContainer(pluginConfig: PluginConfig,
                                 versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {

        val prodSetupCartridgeTask = pluginConfig.getSetupCartridgesTask(PRODUCTION, PROD_ENVS)
        val createConfigProd = pluginConfig.getConfigTask(versionInfoTask, PRODUCTION, PROD_ENVS)

        pluginConfig.configurePackageTask(
            createConfigProd, prodSetupCartridgeTask, CreateMainPackage.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(PRODUCTION)
        prepareTask.configure { task ->
            task.dependsOn(prodSetupCartridgeTask, createConfigProd)
        }
        return prepareTask
    }

    private fun prepareTestContainer(pluginConfig: PluginConfig,
                                     versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {

        val testSetupCartridgeTask = pluginConfig.getSetupCartridgesTask(TEST, TEST_ONLY_ENVS)
        val createConfigTest = pluginConfig.getConfigTask(versionInfoTask, TEST, TEST_ENVS)

        pluginConfig.configurePackageTask(
            createConfigTest, testSetupCartridgeTask, CreateTestPackage.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(TEST)
        prepareTask.configure { task ->
            task.dependsOn(testSetupCartridgeTask, createConfigTest)
        }
        return prepareTask
    }

    private fun prepareServer(pluginConfig: PluginConfig,
                                      versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {

        val setupCartridgeTask = pluginConfig.getSetupCartridgesTask(DEVELOPMENT, DEVELOPMENT_ENVS)
        val createConfig = pluginConfig.getConfigTask(versionInfoTask, DEVELOPMENT, DEVELOPMENT_ENVS)

        val prepareTask = pluginConfig.configurePrepareTask(DEVELOPMENT)
        prepareTask.configure { task ->
            task.dependsOn(setupCartridgeTask, createConfig)
        }
        return prepareTask
    }

    private fun configureAdapterPublishingTasks(project: Project, extension: IntershopExtension) {
        val configZipTask = getZipTasks(
            project = project,
            baseDir = extension.projectConfig.serverDirConfig.base,
            prodDir = extension.projectConfig.serverDirConfig.prod,
            type = "configuration"
        )

        project.afterEvaluate {
            with(project.extensions) {
                project.plugins.withType(MavenPublishPlugin::class.java) {
                    configure(PublishingExtension::class.java) { publishing ->
                        publishing.publications.maybeCreate(
                            extension.mavenPublicationName.get(),
                            MavenPublication::class.java
                        ).apply {
                            artifact(configZipTask.get())
                        }
                    }
                    project.tasks.named("publish").configure {
                            task -> task.dependsOn(configZipTask)
                    }
                }
            }
        }
    }

    private fun getZipTasks(project: Project, baseDir: ServerDir, prodDir: ServerDir, type: String): TaskProvider<Zip> {
        val preparePubTask =
            project.tasks.register("preparePub${type.capitalize()}", PreparePublishDir::class.java) { task ->
                task.baseDirConfig.set(baseDir)
                task.extraDirConfig.set(prodDir)

                task.outputDirectory.set(project.layout.buildDirectory.dir("publish/predir${type}"))
            }

        return project.tasks.register("zip${type.capitalize()}", Zip::class.java) { task ->
            task.from(preparePubTask.get().outputs)

            task.archiveBaseName.set(type)
            task.archiveClassifier.set(type)
            task.destinationDirectory.set(project.layout.buildDirectory.dir("publish/${type}"))
        }
    }

    private fun configureBasePublishingTasks(project: Project, extension: IntershopExtension) {
        project.afterEvaluate {

            var configTask: TaskProvider<Sync>? = null
            try {
                configTask = project.tasks.named(TaskName.PRODUCTION.config(), Sync::class.java)
            } catch(ex: UnknownTaskException) {
                project.logger.debug("No configuration task available.")
            }

            val confZipTask = if(configTask != null ) {
                                    getZipFolder(project, configTask, "configuration")
                                } else {
                                    null
                                }

            if(confZipTask != null) {
                with(project.extensions) {
                    project.plugins.withType(MavenPublishPlugin::class.java) {
                        configure(PublishingExtension::class.java) { publishing ->
                            publishing.publications.maybeCreate(
                                extension.mavenPublicationName.get(),
                                MavenPublication::class.java
                            ).apply {
                                artifact(confZipTask.get())
                            }
                        }
                        if(configTask != null) {
                            project.tasks.named("publish").configure { task -> task.dependsOn(configTask) }
                        }
                    }
                }
            }
        }
    }

    private fun getZipFolder(project: Project, sync: TaskProvider<Sync>, type: String) : TaskProvider<Zip>? {
        return if (! sync.get().outputs.files.isEmpty) {
                project.tasks.register("zip${type.capitalize()}", Zip::class.java) { zip ->
                        zip.from(sync.get().outputs.files)

                        zip.includeEmptyDirs = false

                        zip.archiveFileName.set("${type}.zip")
                        zip.archiveClassifier.set(type)
                        zip.destinationDirectory.set(project.layout.buildDirectory.dir("publish/${type}"))
                    }
                } else { null }
    }
}
