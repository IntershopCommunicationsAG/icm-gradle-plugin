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
import com.intershop.gradle.icm.extension.ProjectConfiguration
import com.intershop.gradle.icm.extension.ServerDir
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.CreateClusterID
import com.intershop.gradle.icm.tasks.CreateConfigFolder
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateSitesFolder
import com.intershop.gradle.icm.tasks.ExtendCartridgeList
import com.intershop.gradle.icm.tasks.PreparePublishDir
import com.intershop.gradle.icm.tasks.ProvideCartridgeListTemplate
import com.intershop.gradle.icm.tasks.ProvideLibFilter
import com.intershop.gradle.icm.tasks.SetupCartridges
import com.intershop.gradle.icm.utils.CartridgeStyle.ALL
import com.intershop.gradle.icm.utils.CartridgeStyle.valueOf
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject

/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(private var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {

        const val PREPARE_SERVER = "prepareServer"
        const val PREPARE_TEST_CONTAINER = "prepareTestContainer"
        const val PREPARE_CONTAINER = "prepareContainer"

        const val PROD_CONTAINER_FOLDER = "container"
        const val TEST_CONTAINER_FOLDER = "testcontainer"
        const val SERVER_FOLDER = "server"

        const val CARTRIDGELIST_FILENAME = "cartridgelist.properties"
        const val CARTRIDGELIST_FOLDER = "cartridgelist"
        const val CARTRIDGELIST = "$CARTRIDGELIST_FOLDER/$CARTRIDGELIST_FILENAME"

        const val CARTRIDGE_FOLDER = "cartridges"
        const val PROJECT_LIBS_FOLDER = "prjlibs"

        const val CREATE_SITESFOLDER_PROD = "createSitesProd"
        const val CREATE_SITESFOLDER_TEST = "createSitesTest"
        const val CREATE_SITESFOLDER = "createSites"

        const val CREATE_CONFIGFOLDER_PROD = "createConfigProd"
        const val CREATE_CONFIGFOLDER_TEST = "createConfigTest"
        const val CREATE_CONFIGFOLDER = "createConfig"

        const val SITES_FOLDER = "sites_folder"
        const val CONFIG_FOLDER = "config_folder"

        const val PROVIDE_CARTRIDGELIST_TEMPLATE = "provideCartridgeListTemplate"

        const val EXTEND_CARTRIDGELIST_PROD = "extendCartridgeListProd"
        const val EXTEND_CARTRIDGELIST_TEST = "extendCartridgeListTest"
        const val EXTEND_CARTRIDGELIST = "extendCartridgeList"

        const val PROVIDE_LIBFILTER = "provideLibFilter"

        const val SETUP_CARTRIDGES_PROD = "setupCartridgesProd"
        const val SETUP_CARTRIDGES_TEST = "setupCartridgesTest"
        const val SETUP_CARTRIDGES = "setupCartridges"


        const val COPY_LIBS_PROD = "copyLibsProd"
        const val COPY_LIBS_TEST = "copyLibsTest"
        const val COPY_LIBS = "copyLibs"

        val PROD_ENVS = listOf(EnvironmentType.PRODUCTION)
        val TEST_ENVS = listOf(EnvironmentType.PRODUCTION, EnvironmentType.TEST)
        val TEST_ONLY_ENVS = listOf(EnvironmentType.TEST)
        val SERVER_ENVS = listOf(EnvironmentType.PRODUCTION, EnvironmentType.DEVELOPMENT, EnvironmentType.TEST)
    }

    override fun apply(project: Project) {
        with(project.rootProject) {

            plugins.apply(ICMBasePlugin::class.java)

            val extension = extensions.findByType(
                IntershopExtension::class.java
            ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

            configureProjectTasks(this, extension.projectConfig)

            if(extension.projectConfig.newBaseProject.get()) {
                configureBasePublishingTasks(this, extension)
            } else {
                configureAdapterPublishingTasks(this, extension)
            }
        }
    }
    
    private fun configureProjectTasks(project: Project,
                                      projectConfig: ProjectConfiguration) {

        val infoTask = project.tasks.getByName(CreateServerInfo.DEFAULT_NAME) as CreateServerInfo
        val templateCartridgeList = getTplCartridgeListTask(project, projectConfig)
        val libfilter = getLibFilterTask(project, projectConfig)

        val prepareContainerTask = prepareContainer(
            project, projectConfig,  libfilter, templateCartridgeList, infoTask)
        val prepareTestContainerTask = prepareTestContainer(
            project, projectConfig, libfilter, templateCartridgeList, infoTask)
        val prepareServerTask = prepareServer(
            project, projectConfig, libfilter, templateCartridgeList, infoTask)

        val copyLibsProd = get3rdPartyCopyTask(
            project = project,
            taskName = COPY_LIBS_PROD,
            targetDescr = "test container",
            targetPath = PROD_CONTAINER_FOLDER)

        prepareContainerTask.dependsOn(copyLibsProd)

        val copyLibsTest = get3rdPartyCopyTask(
            project = project,
            taskName = COPY_LIBS_TEST,
            targetDescr = "container",
            targetPath = TEST_CONTAINER_FOLDER)

        prepareTestContainerTask.dependsOn(copyLibsTest)

        val copyLibs = get3rdPartyCopyTask(
            project = project,
            taskName = COPY_LIBS,
            targetDescr = "local server",
            targetPath = SERVER_FOLDER)

        prepareServerTask.dependsOn(copyLibs)

        project.subprojects { sub ->
            sub.tasks.withType(CopyThirdpartyLibs::class.java) { ctlTask ->

                val styleValue =
                    with(sub.extensions.extraProperties) {
                        if (has("cartridge.style")) { get("cartridge.style").toString() } else { "all" }
                    }
                val style = valueOf(styleValue.toUpperCase())

                ctlTask.provideLibFilterFile(libfilter.outputFile)

                if(style == ALL || SERVER_ENVS.contains(style.environmentType())) {
                    copyLibs.from(ctlTask.outputs.files)
                }
                if(style == ALL || TEST_ONLY_ENVS.contains(style.environmentType())) {
                    copyLibsTest.from(ctlTask.outputs.files)
                }
                if(style == ALL || PROD_ENVS.contains(style.environmentType())) {
                    copyLibsProd.from(ctlTask.outputs.files)
                }
            }
        }
    }

    private fun prepareContainer(project: Project,
                                 projectConfig: ProjectConfiguration,
                                 libFilterTask: ProvideLibFilter,
                                 templateTask: ProvideCartridgeListTemplate,
                                 versionInfoTask: CreateServerInfo): Task {

        val prodSetupCartridgeTask = getSetupCartridgesTask(
            project = project,
            projectConfig = projectConfig,
            taskName = SETUP_CARTRIDGES_PROD,
            libFilterTask = libFilterTask,
            environmentTypesList = PROD_ENVS,
            targetPath = PROD_CONTAINER_FOLDER)

        val createSitesProd = getSitesTask(
            project = project,
            projectConfig = projectConfig,
            taskName = CREATE_SITESFOLDER_PROD,
            targetPath = PROD_CONTAINER_FOLDER,
            extraServerDir = projectConfig.serverDirConfig.prod.sites)

        val createConfigProd = getConfigTask(
            project = project,
            projectConfig = projectConfig,
            templateTask = templateTask,
            versionInfoTask = versionInfoTask,
            cartridgeListTaskName = EXTEND_CARTRIDGELIST_PROD,
            createConfigTaskName = CREATE_CONFIGFOLDER_PROD,
            environmentTypesList = PROD_ENVS,
            targetPath = PROD_CONTAINER_FOLDER,
            extraServerDir = projectConfig.serverDirConfig.prod.config)

        return project.tasks.maybeCreate(PREPARE_CONTAINER).apply {
            group = IntershopExtension.INTERSHOP_GROUP_NAME
            description = "starts all tasks for the preparation of a container build"

            dependsOn(prodSetupCartridgeTask, createSitesProd, createConfigProd)
        }
    }

    private fun prepareTestContainer(project: Project,
                                     projectConfig: ProjectConfiguration,
                                     libFilterTask: ProvideLibFilter,
                                     templateTask: ProvideCartridgeListTemplate,
                                     versionInfoTask: CreateServerInfo): Task {
        val testSetupCartridgeTask = getSetupCartridgesTask(
            project = project,
            projectConfig = projectConfig,
            taskName = SETUP_CARTRIDGES_TEST,
            libFilterTask = libFilterTask,
            environmentTypesList = TEST_ONLY_ENVS,
            targetPath = TEST_CONTAINER_FOLDER)

        val createSitesTest = getSitesTask(
            project = project,
            projectConfig = projectConfig,
            taskName = CREATE_SITESFOLDER_TEST,
            targetPath = TEST_CONTAINER_FOLDER,
            extraServerDir = projectConfig.serverDirConfig.test.sites)

        val createConfigTest = getConfigTask(
            project = project,
            projectConfig = projectConfig,
            templateTask = templateTask,
            versionInfoTask = versionInfoTask,
            cartridgeListTaskName = EXTEND_CARTRIDGELIST_TEST,
            createConfigTaskName = CREATE_CONFIGFOLDER_TEST,
            environmentTypesList = TEST_ENVS,
            targetPath = TEST_CONTAINER_FOLDER,
            extraServerDir = projectConfig.serverDirConfig.test.config)

        return project.tasks.maybeCreate(PREPARE_TEST_CONTAINER).apply {
            group = IntershopExtension.INTERSHOP_GROUP_NAME
            description = "starts all tasks for the preparation of a test container build"

            dependsOn(testSetupCartridgeTask, createSitesTest, createConfigTest)
        }
    }

    private fun prepareServer(project: Project,
                              projectConfig: ProjectConfiguration,
                              libFilterTask: ProvideLibFilter,
                              templateTask: ProvideCartridgeListTemplate,
                              versionInfoTask: CreateServerInfo): Task {

        val setupCartridgeTask = getSetupCartridgesTask(
            project = project,
            projectConfig = projectConfig,
            taskName = SETUP_CARTRIDGES,
            libFilterTask = libFilterTask,
            environmentTypesList = SERVER_ENVS,
            targetPath = SERVER_FOLDER)

        val createSites = getSitesTask(
            project = project,
            projectConfig = projectConfig,
            taskName = CREATE_SITESFOLDER,
            targetPath = SERVER_FOLDER,
            extraServerDir = projectConfig.serverDirConfig.dev.sites)

        val createConfig = getConfigTask(
            project = project,
            projectConfig = projectConfig,
            templateTask = templateTask,
            versionInfoTask = versionInfoTask,
            cartridgeListTaskName = EXTEND_CARTRIDGELIST,
            createConfigTaskName = CREATE_CONFIGFOLDER,
            environmentTypesList = SERVER_ENVS,
            targetPath = SERVER_FOLDER,
            extraServerDir = projectConfig.serverDirConfig.dev.config)

        val createClusterID = project.tasks.findByName(CreateClusterID.DEFAULT_NAME)

        return project.tasks.maybeCreate(PREPARE_SERVER).apply {
            group = IntershopExtension.INTERSHOP_GROUP_NAME
            description = "starts all tasks for the preparation of a local server"

            dependsOn(setupCartridgeTask, createSites, createConfig, createClusterID)
        }
    }

    private fun getTplCartridgeListTask(project: Project,
                                        projectConfig: ProjectConfiguration): ProvideCartridgeListTemplate {
        with(project) {
            return tasks.maybeCreate(
                PROVIDE_CARTRIDGELIST_TEMPLATE,
                ProvideCartridgeListTemplate::class.java
            ).apply {
                provideBaseDependency(projectConfig.base.dependency)
                provideFileDependency(projectConfig.cartridgeListDependency)
            }
        }
    }

    private fun getLibFilterTask(project: Project,
                                 projectConfig: ProjectConfiguration): ProvideLibFilter {
        with(project) {
            return tasks.maybeCreate(
                PROVIDE_LIBFILTER,
                ProvideLibFilter::class.java
            ).apply {
                provideBaseDependency(projectConfig.base.dependency)
                provideFileDependency(projectConfig.libFilterFileDependency)
            }
        }
    }

    private fun getSetupCartridgesTask(project: Project,
                                       projectConfig: ProjectConfiguration,
                                       taskName: String,
                                       libFilterTask: ProvideLibFilter,
                                       environmentTypesList: List<EnvironmentType>,
                                       targetPath: String): SetupCartridges {
        return with(project) {
            tasks.maybeCreate(
                taskName,
                SetupCartridges::class.java
            ).apply {
                provideCartridges(projectConfig.cartridges)
                provideDBprepareCartridges(projectConfig.dbprepareCartridges)
                provideLibFilterFile(libFilterTask.outputFile)
                environmentTypes.set(environmentTypesList)
                provideOutputDir(projectLayout.buildDirectory.dir("$targetPath/$CARTRIDGE_FOLDER"))
            }
        }
    }

    private fun get3rdPartyCopyTask(project: Project,
                                    taskName: String,
                                    targetDescr: String,
                                    targetPath: String): Sync {
        with(project) {
            return tasks.maybeCreate(
                taskName,
                Sync::class.java
            ).apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "Copy all 3rd party libs to one folder for a $targetDescr"

                this.into(projectLayout.buildDirectory.dir("$targetPath/$PROJECT_LIBS_FOLDER"))
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }

    private fun getSitesTask(project: Project,
                             projectConfig: ProjectConfiguration,
                             taskName: String,
                             targetPath: String,
                             extraServerDir: ServerDir): CreateSitesFolder {
        with(project) {
            return tasks.maybeCreate(
                taskName,
                CreateSitesFolder::class.java
            ).apply {
                baseProject.set(projectConfig.base)

                projectConfig.modules.all {
                    module(it)
                }

                baseDirConfig.set(projectConfig.serverDirConfig.base.sites)
                extraDirConfig.set(extraServerDir)

                provideOutputDir(projectLayout.buildDirectory.dir("$targetPath/$SITES_FOLDER"))
            }
        }
    }

    private fun getConfigTask(project: Project,
                              projectConfig: ProjectConfiguration,
                              templateTask: ProvideCartridgeListTemplate,
                              versionInfoTask: CreateServerInfo,
                              cartridgeListTaskName: String,
                              createConfigTaskName: String,
                              environmentTypesList: List<EnvironmentType>,
                              targetPath: String,
                              extraServerDir: ServerDir): CreateConfigFolder {
        with(project) {

            val cartridgeListTask = tasks.maybeCreate(cartridgeListTaskName, ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateTask.outputFile)
                provideCartridges(projectConfig.cartridges)
                provideDBprepareCartridges(projectConfig.dbprepareCartridges)

                environmentTypes.set(environmentTypesList)

                provideOutputFile(project.layout.buildDirectory.file(("$targetPath/$CARTRIDGELIST")))
            }



            return tasks.maybeCreate(createConfigTaskName, CreateConfigFolder::class.java).apply {
                baseProject.set(projectConfig.base)

                projectConfig.modules.all {
                    module(it)
                }

                baseDirConfig.set(projectConfig.serverDirConfig.base.config)
                extraDirConfig.set(extraServerDir)

                provideCartridgeListFile(cartridgeListTask.outputFile)
                provideVersionInfoFile(versionInfoTask.outputFile)

                provideOutputDir(projectLayout.buildDirectory.dir("$targetPath/$CONFIG_FOLDER"))
            }
        }
    }

    private fun configureAdapterPublishingTasks(project: Project, extension: IntershopExtension) {
        val configZipTask = getZipTasks(project = project,
            baseDir = extension.projectConfig.serverDirConfig.base.config,
            prodDir = extension.projectConfig.serverDirConfig.prod.config,
            type = "configuration")

        val sitesZipTask = getZipTasks(project = project,
            baseDir = extension.projectConfig.serverDirConfig.base.sites,
            prodDir = extension.projectConfig.serverDirConfig.prod.sites,
            type = "sites")

        project.afterEvaluate {
                with(project.extensions) {
                    project.plugins.withType(MavenPublishPlugin::class.java) {
                        configure(PublishingExtension::class.java) { publishing ->
                            publishing.publications.maybeCreate(
                                extension.mavenPublicationName.get(),
                                MavenPublication::class.java
                            ).apply {
                                artifact(configZipTask)
                                artifact(sitesZipTask)
                            }
                        }
                        project.tasks.getByName("publish").dependsOn(configZipTask)
                        project.tasks.getByName("publish").dependsOn(sitesZipTask)
                    }
                }

        }
    }

    private fun getZipTasks(project: Project, baseDir: ServerDir, prodDir: ServerDir, type: String): Zip {
        with(project) {
            val preparePubTask =
                tasks.maybeCreate("preparePub${type.capitalize()}", PreparePublishDir::class.java).apply {
                    this.baseDirConfig.set(baseDir)
                    this.extraDirConfig.set(prodDir)

                    this.outputDirectory.set(project.layout.buildDirectory.dir("publish/predir${type}"))
                }

            return tasks.maybeCreate("zip${type.capitalize()}", Zip::class.java).apply {
                this.from(preparePubTask.outputs)

                this.archiveBaseName.set(type)
                this.archiveClassifier.set(type)
                this.destinationDirectory.set(project.layout.buildDirectory.dir("publish/${type}"))
            }
        }
    }

    private fun configureBasePublishingTasks(project: Project, extension: IntershopExtension) {
        project.afterEvaluate {

            val configTask = project.tasks.findByName(CREATE_CONFIGFOLDER_PROD)
            val sitesTask = project.tasks.findByName(CREATE_SITESFOLDER_PROD)

            val confZipTask = if(configTask != null ) {
                                    getZipFolder(project, configTask.outputs.files, "configuration")
                                } else {
                                    null
                                }
            val sitesZipTask = if(sitesTask != null) {
                                    getZipFolder(project, sitesTask.outputs.files, "sites")
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
                                    artifact(confZipTask)
                                }
                                if(sitesZipTask != null) {
                                    artifact(sitesZipTask)
                                }
                            }
                        }
                        if(configTask != null) {
                            project.tasks.getByName("publish").dependsOn(configTask)
                        }
                        if(sitesTask != null) {
                            project.tasks.getByName("publish").dependsOn(sitesTask)
                        }
                    }
                }
            }
        }
    }

    private fun getZipFolder(project: Project, fc: FileCollection, type: String) : Task? {
        with(project) {
            if (! fc.isEmpty) {
                return tasks.maybeCreate("zip${type.capitalize()}", Zip::class.java).apply {
                    this.from(fc)

                    this.includeEmptyDirs = false

                    this.archiveFileName.set("${type}.zip")
                    this.archiveClassifier.set(type)
                    this.destinationDirectory.set(project.layout.buildDirectory.dir("publish/${type}"))
                }
            }
        }
        return null
    }
}
