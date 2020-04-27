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
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.CreateConfigFolder
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import com.intershop.gradle.icm.tasks.CreateSitesFolder
import com.intershop.gradle.icm.tasks.ExtendCartridgeList
import com.intershop.gradle.icm.tasks.ProvideCartridgeListTemplate
import com.intershop.gradle.icm.tasks.ProvideLibFilter
import com.intershop.gradle.icm.tasks.SetupCartridges
import com.intershop.gradle.icm.utils.CartridgeStyle.ALL
import com.intershop.gradle.icm.utils.CartridgeStyle.valueOf
import com.intershop.gradle.icm.utils.CopySpecUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.ProjectLayout
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Sync
import javax.inject.Inject

/**
 * The main plugin class of this plugin.
 */
open class ICMProjectPlugin @Inject constructor(private var projectLayout: ProjectLayout) : Plugin<Project> {

    companion object {
        /**
         * Path for original cartridge list properties in build directory.
         */
        const val CARTRIDGELIST_FILENAME = "cartridgelist.properties"
        const val CARTRIDGELIST_FOLDER = "cartridgelist"

        const val PROD_CONTAINER_FOLDER = "container"
        const val TEST_CONTAINER_FOLDER = "testcontainer"
        const val SERVER_FOLDER = "server"

        const val CARTRIDGE_FOLDER = "cartridges"
        const val PROJECT_LIBS_FOLDER = "prjlibs"

        const val CREATE_SITESFOLDER_PROD = "createSitesProd"
        const val CREATE_SITESFOLDER_TEST = "createSitesTest"
        const val CREATE_SITESFOLDER = "createSites"

        const val CREATE_CONFIGFOLDER_PROD = "createConfigProd"
        const val CREATE_CONFIGFOLDER_TEST = "createConfigTest"
        const val CREATE_CONFIGFOLDER = "createConfig"

        const val SITES_FOLDER = "sites"
        const val CONFIG_FOLDER = "system-conf"

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
            val extension = extensions.findByType(
                IntershopExtension::class.java
            ) ?: extensions.create(IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java)

            val prepareTask = tasks.maybeCreate("prepareServer").apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "starts all tasks for the preparation of a local server"
            }

            val prepareContainerTask = tasks.maybeCreate("prepareContainer").apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "starts all tasks for the preparation of a container build"
            }

            val prepareTestContainerTask = tasks.maybeCreate("prepareTestContainer").apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "starts all tasks for the preparation of a test container build"
            }

            val infoTask = configureInfoTask(this, extension)

            configureExtCartridgeTask(this, extension, prepareTask, prepareContainerTask, prepareTestContainerTask)

            configureFolderTasks(this, extension)
        }
    }

    private fun configureInfoTask(project: Project,
                                  extension: IntershopExtension) : CreateServerInfoProperties {
        with(project) {
            return tasks.maybeCreate(
                CreateServerInfoProperties.DEFAULT_NAME,
                CreateServerInfoProperties::class.java
            ).apply {
                this.provideProductId(extension.projectInfo.productIDProvider)
                this.provideProductName(extension.projectInfo.productNameProvider)
                this.provideCopyrightOwner(extension.projectInfo.copyrightOwnerProvider)
                this.provideCopyrightFrom(extension.projectInfo.copyrightFromProvider)
                this.provideOrganization(extension.projectInfo.organizationProvider)
            }
        }
    }

    private fun configureExtCartridgeTask(project: Project,
                                          extension: IntershopExtension,
                                          prepareTask: Task,
                                          prepareContainerTask: Task,
                                          prepareTestContainerTask: Task) {
        with(project) {
            val templateCartridgeList = tasks.maybeCreate(
                                                PROVIDE_CARTRIDGELIST_TEMPLATE,
                                                ProvideCartridgeListTemplate::class.java).apply {
                provideBaseDependency(extension.projectConfig.base.dependencyProvider)
                provideFileDependency(extension.projectConfig.cartridgeListDependencyProvider)
            }

            val cartridgeListTaskProd = tasks.maybeCreate(EXTEND_CARTRIDGELIST_PROD, ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateCartridgeList.outputFile)
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                environmentTypes.set(PROD_ENVS)

                provideOutputFile(project.layout.buildDirectory.file(("${PROD_CONTAINER_FOLDER}/${CARTRIDGELIST_FOLDER}/${CARTRIDGELIST_FILENAME}")))
            }

            val cartridgeListTaskTest = tasks.maybeCreate(EXTEND_CARTRIDGELIST_TEST, ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateCartridgeList.outputFile)
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                environmentTypes.set(TEST_ENVS)

                provideOutputFile(project.layout.buildDirectory.file(("${TEST_CONTAINER_FOLDER}/${CARTRIDGELIST_FOLDER}/${CARTRIDGELIST_FILENAME}")))
            }

            val cartridgeListTask = tasks.maybeCreate(EXTEND_CARTRIDGELIST, ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateCartridgeList.outputFile)
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                environmentTypes.set(SERVER_ENVS)

                provideOutputFile(project.layout.buildDirectory.file(("${SERVER_FOLDER}/${CARTRIDGELIST_FOLDER}/${CARTRIDGELIST_FILENAME}")))
            }


            val libfilter = tasks.maybeCreate(PROVIDE_LIBFILTER, ProvideLibFilter::class.java).apply {
                provideBaseDependency(extension.projectConfig.base.dependencyProvider)
                provideFileDependency(extension.projectConfig.libFilterFileDependencyProvider)
            }

            tasks.maybeCreate(SETUP_CARTRIDGES_PROD, SetupCartridges::class.java).apply {
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)
                provideLibFilterFile(libfilter.outputFile)

                environmentTypes.set(PROD_ENVS)

                provideOutputDir(projectLayout.buildDirectory.dir("${PROD_CONTAINER_FOLDER}/${CARTRIDGE_FOLDER}"))
                prepareContainerTask.dependsOn(this)
            }

            tasks.maybeCreate(SETUP_CARTRIDGES_TEST, SetupCartridges::class.java).apply {
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                provideLibFilterFile(libfilter.outputFile)

                environmentTypes.set(TEST_ONLY_ENVS)

                provideOutputDir(projectLayout.buildDirectory.dir("${TEST_CONTAINER_FOLDER}/${CARTRIDGE_FOLDER}"))
                prepareTestContainerTask.dependsOn(this)
            }

            tasks.maybeCreate(SETUP_CARTRIDGES, SetupCartridges::class.java).apply {
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                provideLibFilterFile(libfilter.outputFile)

                environmentTypes.set(SERVER_ENVS)

                provideOutputDir(projectLayout.buildDirectory.dir("${SERVER_FOLDER}/${CARTRIDGE_FOLDER}"))

                prepareTask.dependsOn(this)
            }

            val copyLibsProd = tasks.maybeCreate(COPY_LIBS_PROD, Sync::class.java).apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "Copy all 3rd party libs to one folder for a test container"

                this.into(projectLayout.buildDirectory.dir("${PROD_CONTAINER_FOLDER}/${PROJECT_LIBS_FOLDER}"))
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                prepareTestContainerTask.dependsOn(this)
            }

            val copyLibsTest = tasks.maybeCreate(COPY_LIBS_TEST, Sync::class.java).apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "Copy all 3rd party libs to one folder for a container"

                this.into(projectLayout.buildDirectory.dir("${TEST_CONTAINER_FOLDER}/${PROJECT_LIBS_FOLDER}"))
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                prepareContainerTask.dependsOn(this)
            }

            val copyLibs = tasks.maybeCreate(COPY_LIBS, Sync::class.java).apply {
                group = IntershopExtension.INTERSHOP_GROUP_NAME
                description = "Copy all 3rd party libs to one folder for a local server"

                this.into(projectLayout.buildDirectory.dir("${SERVER_FOLDER}/${PROJECT_LIBS_FOLDER}}"))
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                prepareTask.dependsOn(this)
            }

            subprojects { sub ->
                sub.tasks.withType(CopyThirdpartyLibs::class.java) { ctlTask ->

                    val styleValue =
                        with(sub.extensions.extraProperties)
                            { if (has("cartridge.style")) { get("cartridge.style").toString() } else { "all" }
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

            // create sites
            val createSitesProd = tasks.maybeCreate(CREATE_SITESFOLDER_PROD, CreateSitesFolder::class.java).apply {
                baseProject.set(extension.projectConfig.base)
                modules.set(extension.projectConfig.modules.asMap)

                baseDirConfig.set(extension.projectConfig.serverDirConfig.base.sites)
                extraDirConfig.set(extension.projectConfig.serverDirConfig.prod.sites)

                provideOutputDir(projectLayout.buildDirectory.dir("${PROD_CONTAINER_FOLDER}/${SITES_FOLDER}"))
            }
            val createSitesTest = tasks.maybeCreate(CREATE_SITESFOLDER_TEST, CreateSitesFolder::class.java).apply {
                baseProject.set(extension.projectConfig.base)
                modules.set(extension.projectConfig.modules.asMap)

                baseDirConfig.set(extension.projectConfig.serverDirConfig.base.sites)
                extraDirConfig.set(extension.projectConfig.serverDirConfig.test.sites)

                provideOutputDir(projectLayout.buildDirectory.dir("${TEST_CONTAINER_FOLDER}/${SITES_FOLDER}"))
            }
            val createSites = tasks.maybeCreate(CREATE_SITESFOLDER, CreateSitesFolder::class.java).apply {
                baseProject.set(extension.projectConfig.base)
                modules.set(extension.projectConfig.modules.asMap)

                baseDirConfig.set(extension.projectConfig.serverDirConfig.base.sites)
                extraDirConfig.set(extension.projectConfig.serverDirConfig.dev.sites)

                provideOutputDir(projectLayout.buildDirectory.dir("${SERVER_FOLDER}/${SITES_FOLDER}"))
            }

            // create conf
            val createConfigProd = tasks.maybeCreate(CREATE_CONFIGFOLDER_PROD, CreateConfigFolder::class.java).apply {
                baseProject.set(extension.projectConfig.base)
                modules.set(extension.projectConfig.modules.asMap)

                baseDirConfig.set(extension.projectConfig.serverDirConfig.base.config)
                extraDirConfig.set(extension.projectConfig.serverDirConfig.prod.config)

                provideCartridgeListFile(cartridgeListTaskProd.outputFile)

                provideOutputDir(projectLayout.buildDirectory.dir("${PROD_CONTAINER_FOLDER}/${CONFIG_FOLDER}"))
            }
            val createConfigTest = tasks.maybeCreate(CREATE_CONFIGFOLDER_TEST, CreateConfigFolder::class.java).apply {
                baseProject.set(extension.projectConfig.base)
                modules.set(extension.projectConfig.modules.asMap)

                baseDirConfig.set(extension.projectConfig.serverDirConfig.base.config)
                extraDirConfig.set(extension.projectConfig.serverDirConfig.test.config)

                provideCartridgeListFile(cartridgeListTaskTest.outputFile)

                provideOutputDir(projectLayout.buildDirectory.dir("${TEST_CONTAINER_FOLDER}/${CONFIG_FOLDER}"))
            }
            val createConfig = tasks.maybeCreate(CREATE_CONFIGFOLDER, CreateConfigFolder::class.java).apply {
                baseProject.set(extension.projectConfig.base)
                modules.set(extension.projectConfig.modules.asMap)

                baseDirConfig.set(extension.projectConfig.serverDirConfig.base.config)
                extraDirConfig.set(extension.projectConfig.serverDirConfig.dev.config)

                provideCartridgeListFile(cartridgeListTaskProd.outputFile)

                provideOutputDir(projectLayout.buildDirectory.dir("${SERVER_FOLDER}/${CONFIG_FOLDER}"))
            }
        }
    }

    private fun configureFolderTasks(project: Project, extension: IntershopExtension) {
        project.afterEvaluate {
            val confTask = configureConfFolder(it, extension)
            val sitesFolder = configureSitesFolder(it, extension)
            if(confTask != null || sitesFolder != null) {
                with(project.extensions) {
                    project.plugins.withType(MavenPublishPlugin::class.java) {
                        configure(PublishingExtension::class.java) { publishing ->
                            publishing.publications.maybeCreate(
                                extension.mavenPublicationName,
                                MavenPublication::class.java
                            ).apply {
                                if(confTask != null) {
                                    artifact(confTask)
                                }
                                if(sitesFolder != null) {
                                    artifact(sitesFolder)
                                }
                            }
                        }
                        if(confTask != null) {
                            project.tasks.getByName("publish").dependsOn(confTask)
                        }
                        if(sitesFolder != null) {
                            project.tasks.getByName("publish").dependsOn(sitesFolder)
                        }
                    }
                }
            }
        }
    }

    private fun configureConfFolder(project: Project, extension: IntershopExtension) : Task? {
        with(project) {
            val conf = extension.projectConfig.serverDirConfig.base.config
            if (conf.dirs.isNotEmpty()) {
                return tasks.maybeCreate("zipConf", org.gradle.api.tasks.bundling.Zip::class.java).apply {
                    this.with(CopySpecUtil.getCSForServerDir(project, conf))

                    this.includeEmptyDirs = false

                    this.archiveFileName.set("configuration.zip")
                    this.archiveClassifier.set("configuration")
                    this.destinationDirectory.set(project.layout.buildDirectory.dir("publish/configuration"))
                }
            }
        }
        return null
    }

    private fun configureSitesFolder(project: Project, extension: IntershopExtension) : Task? {
        with(project) {
            val conf = extension.projectConfig.serverDirConfig.base.sites
            if (conf.dirs.isNotEmpty()) {
                return tasks.maybeCreate("zipSites", org.gradle.api.tasks.bundling.Zip::class.java).apply {
                    this.with(CopySpecUtil.getCSForServerDir(project, conf))

                    this.includeEmptyDirs = false

                    this.archiveFileName.set("sites.zip")
                    this.archiveClassifier.set("sites")
                    this.destinationDirectory.set(project.layout.buildDirectory.dir("publish/sites"))
                }
            }
        }
        return null
    }
}

