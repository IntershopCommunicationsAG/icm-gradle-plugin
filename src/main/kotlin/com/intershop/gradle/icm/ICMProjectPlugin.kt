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

import com.intershop.gradle.icm.ICMBasePlugin.Companion.TASK_WRITECARTRIDGEFILES
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.extension.ServerDir
import com.intershop.gradle.icm.project.PluginConfig
import com.intershop.gradle.icm.project.TaskConfCopyLib
import com.intershop.gradle.icm.project.TaskName
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.CreateClusterID
import com.intershop.gradle.icm.tasks.CreateInitPackage
import com.intershop.gradle.icm.tasks.CreateInitTestPackage
import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.PreparePublishDir
import com.intershop.gradle.icm.utils.CartridgeStyle.ALL
import com.intershop.gradle.icm.utils.CartridgeStyle.valueOf
import com.intershop.gradle.icm.utils.EnvironmentType.DEVELOPMENT
import com.intershop.gradle.icm.utils.EnvironmentType.PRODUCTION
import com.intershop.gradle.icm.utils.EnvironmentType.TEST
import com.intershop.gradle.isml.IsmlPlugin
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

            configureProjectTasks(this, pluginConfig)

            if(extension.projectConfig.newBaseProject.get()) {
                configureBasePublishingTasks(this, extension)
            } else {
                configureAdapterPublishingTasks(this, extension)
            }
        }
    }

    private fun configureProjectTasks(project: Project, pluginConfig: PluginConfig) {
        val infoTask = project.tasks.named(CreateServerInfo.DEFAULT_NAME) as TaskProvider<CreateServerInfo>
        val writeCartridgeFile = project.tasks.named(TASK_WRITECARTRIDGEFILES)
        pluginConfig.getCartridgeListTemplate()

        val copyLibsProd = pluginConfig.get3rdPartyCopyTask(TaskConfCopyLib.PRODUCTION)
        val prepareContainer = prepareContainer(pluginConfig, infoTask, copyLibsProd)
        prepareContainer.configure { task -> task.dependsOn(copyLibsProd, writeCartridgeFile) }

        val copyLibsTest = pluginConfig.get3rdPartyCopyTask(TaskConfCopyLib.TEST)
        val prepareTestContainer = prepareTestContainer(pluginConfig, infoTask, copyLibsTest)
        prepareTestContainer.configure { task -> task.dependsOn(copyLibsTest, writeCartridgeFile) }

        val copyLibs = pluginConfig.get3rdPartyCopyTask(TaskConfCopyLib.DEVELOPMENT)
        val prepareServer = prepareServer(project, pluginConfig, infoTask)
        prepareServer.configure { task -> task.dependsOn(copyLibs, writeCartridgeFile) }

        configurePrepareTasks(pluginConfig, prepareServer, prepareTestContainer, prepareContainer)
        configureCopyLibsTasks(pluginConfig, copyLibs, copyLibsTest, copyLibsProd)
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
            sub.plugins.withType(IsmlPlugin::class.java) {
                val ismlTask = sub.tasks.getByName("isml2classMain")
                prepareServer.configure { task -> task.dependsOn(ismlTask) }
                prepareTestContainer.configure { task -> task.dependsOn(ismlTask) }
                prepareContainer.configure { task -> task.dependsOn(ismlTask) }
            }
        }
    }

    private fun configureCopyLibsTasks(pluginConfig: PluginConfig,
                                       copyLibs: TaskProvider<Sync>,
                                       copyLibsTest: TaskProvider<Sync>,
                                       copyLibsProd: TaskProvider<Sync>) {
        pluginConfig.project.subprojects { sub ->
            sub.tasks.withType(CopyThirdpartyLibs::class.java) { ctlTask ->

                val styleValue =
                    with(sub.extensions.extraProperties) {
                        if (has("cartridge.style")) {
                            get("cartridge.style").toString()
                        } else {
                            "all"
                        }
                    }
                val style = valueOf(styleValue.toUpperCase())

                ctlTask.provideLibFilterFile(pluginConfig.getLibFilterFile().outputFile)

                if (style == ALL || DEVELOPMENT_ENVS.contains(style.environmentType())) {
                    copyLibs.configure { task -> task.from(ctlTask.outputs.files) }
                }
                if (style == ALL || TEST_ONLY_ENVS.contains(style.environmentType())) {
                    copyLibsTest.configure { task -> task.from(ctlTask.outputs.files) }
                }
                if (style == ALL || PROD_ENVS.contains(style.environmentType())) {
                    copyLibsProd.configure { task -> task.from(ctlTask.outputs.files) }
                }
            }
        }
    }

    private fun prepareContainer(pluginConfig: PluginConfig,
                                 versionInfoTask: TaskProvider<CreateServerInfo>,
                                 copyLibs: TaskProvider<Sync>): TaskProvider<Task> {

        val prodSetupCartridgeTask = pluginConfig.getSetupCartridgesTask(PRODUCTION, PROD_ENVS)
        val createSitesProd = pluginConfig.getSitesTask(PRODUCTION)
        val createConfigProd = pluginConfig.getConfigTask(versionInfoTask, PRODUCTION, PROD_ENVS)

        pluginConfig.configureInitTask(createSitesProd, CreateInitPackage.DEFAULT_NAME)
        pluginConfig.configurePackageTask(
            createConfigProd, prodSetupCartridgeTask, copyLibs, CreateMainPackage.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(PRODUCTION)
        prepareTask.configure { task ->
            task.dependsOn(prodSetupCartridgeTask, createSitesProd, createConfigProd)
        }
        return prepareTask
    }

    private fun prepareTestContainer(pluginConfig: PluginConfig,
                                     versionInfoTask: TaskProvider<CreateServerInfo>,
                                     copyLibs: TaskProvider<Sync>): TaskProvider<Task> {

        val testSetupCartridgeTask = pluginConfig.getSetupCartridgesTask(TEST, TEST_ONLY_ENVS)
        val createSitesTest = pluginConfig.getSitesTask(TEST)
        val createConfigTest = pluginConfig.getConfigTask(versionInfoTask, TEST, TEST_ENVS)

        pluginConfig.configureInitTask(createSitesTest, CreateInitTestPackage.DEFAULT_NAME)
        pluginConfig.configurePackageTask(
            createConfigTest, testSetupCartridgeTask, copyLibs, CreateTestPackage.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(TEST)
        prepareTask.configure { task ->
            task.dependsOn(testSetupCartridgeTask, createSitesTest, createConfigTest)
        }
        return prepareTask
    }

    private fun prepareServer(project: Project, pluginConfig: PluginConfig,
                                      versionInfoTask: TaskProvider<CreateServerInfo>): TaskProvider<Task> {

        val setupCartridgeTask = pluginConfig.getSetupCartridgesTask(DEVELOPMENT, DEVELOPMENT_ENVS)
        val createSites = pluginConfig.getSitesTask(DEVELOPMENT)
        val createConfig = pluginConfig.getConfigTask(versionInfoTask, DEVELOPMENT, DEVELOPMENT_ENVS)

        val createClusterID = project.tasks.named(CreateClusterID.DEFAULT_NAME)

        val prepareTask = pluginConfig.configurePrepareTask(DEVELOPMENT)
        prepareTask.configure { task ->
            task.dependsOn(setupCartridgeTask, createSites, createConfig, createClusterID)
        }
        return prepareTask
    }

    private fun configureAdapterPublishingTasks(project: Project, extension: IntershopExtension) {
        val configZipTask = getZipTasks(
            project = project,
            baseDir = extension.projectConfig.serverDirConfig.base.config,
            prodDir = extension.projectConfig.serverDirConfig.prod.config,
            type = "configuration"
        )

        val sitesZipTask = getZipTasks(
            project = project,
            baseDir = extension.projectConfig.serverDirConfig.base.sites,
            prodDir = extension.projectConfig.serverDirConfig.prod.sites,
            type = "sites"
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
                            artifact(sitesZipTask.get())
                        }
                    }
                    project.tasks.named("publish").configure {
                            task -> task.dependsOn(configZipTask, sitesZipTask)
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
                configTask = project.tasks.named(TaskName.PRODUCTION.config()) as TaskProvider<Sync>
            } catch(ex: UnknownTaskException) {}
            var sitesTask: TaskProvider<Sync>? = null
            try {
                sitesTask = project.tasks.named(TaskName.PRODUCTION.sites()) as TaskProvider<Sync>
            } catch(ex: UnknownTaskException) {}

            val confZipTask = if(configTask != null ) {
                                    getZipFolder(project, configTask, "configuration")
                                } else {
                                    null
                                }
            val sitesZipTask = if(sitesTask != null) {
                                    getZipFolder(project, sitesTask, "sites")
                                } else {
                                    null
                                }

            if(confZipTask != null || sitesZipTask != null) {
                with(project.extensions) {
                    project.plugins.withType(MavenPublishPlugin::class.java) {
                        configure(PublishingExtension::class.java) { publishing ->
                            publishing.publications.maybeCreate(
                                extension.mavenPublicationName.get(),
                                MavenPublication::class.java
                            ).apply {
                                if(confZipTask != null) {
                                    artifact(confZipTask.get())
                                }
                                if(sitesZipTask != null) {
                                    artifact(sitesZipTask.get())
                                }
                            }
                        }
                        if(configTask != null) {
                            project.tasks.named("publish").configure { task -> task.dependsOn(configTask) }
                        }
                        if(sitesTask != null) {
                            project.tasks.named("publish").configure { task -> task.dependsOn(sitesTask) }
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
