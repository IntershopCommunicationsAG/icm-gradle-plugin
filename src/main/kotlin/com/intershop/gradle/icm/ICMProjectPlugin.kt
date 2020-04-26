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
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import com.intershop.gradle.icm.tasks.CreateSitesFolder
import com.intershop.gradle.icm.tasks.ExtendCartridgeList
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

        const val CREATE_SITES_FOLDER = "createSites"
        const val CREATE_CONF_FOLDER = "createConfig"

        const val CREATE_DEVSITES_FOLDER = "createDevSites"
        const val CREATE_DEVCONF_FOLDER = "createDevConfig"

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

            configureDevTasks(this, extension, prepareTask, infoTask)
            configureContainerTasks(this, extension, prepareContainerTask, infoTask)

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

    private fun configureDevTasks(project: Project,
                                  extension: IntershopExtension,
                                  prepareTask: Task,
                                  infoTask: CreateServerInfoProperties) {
        /**
        with(project) {
            tasks.maybeCreate(CREATE_DEVCONF_FOLDER, CreateConfFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.dirConf = extension.projectConfig.conf
                this.devDirConf = extension.projectConfig.devConf
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("server/configuration"))

                this.provideCartridges(extension.projectConfig.cartridgesProvider)
                this.provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)


                this.writeDevConf = true
                this.provideVersionInfoFile(infoTask.outputFileProperty)
                prepareTask.dependsOn(this)
                this.dependsOn(infoTask)
            }

            tasks.maybeCreate(CREATE_DEVSITES_FOLDER, CreateSitesFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.dirConf = extension.projectConfig.sites
                this.devDirConf = extension.projectConfig.devSites
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("server/sites"))

                prepareTask.dependsOn(this)
            }
        }
        **/
    }

    private fun configureContainerTasks(project: Project,
                                         extension: IntershopExtension,
                                         prepareContainerTask: Task,
                                        infoTask: CreateServerInfoProperties) {
        /**
        with(project) {
            tasks.maybeCreate(CREATE_CONF_FOLDER, CreateConfFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.dirConf = extension.projectConfig.conf
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("container/configuration"))

                this.provideCartridges(extension.projectConfig.cartridgesProvider)
                this.provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                this.writeDevConf = false
                this.provideVersionInfoFile(infoTask.outputFileProperty)
                prepareContainerTask.dependsOn(this)
                this.dependsOn(infoTask)
            }

            tasks.maybeCreate(CREATE_SITES_FOLDER, CreateSitesFolder::class.java).apply {
                this.baseProjects = extension.projectConfig.baseProjects.asMap
                this.dirConf = extension.projectConfig.sites
                this.outputDirProperty.set(projectLayout.buildDirectory.dir("container/sites"))

                prepareContainerTask.dependsOn(this)
            }
        }
        **/
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

            tasks.maybeCreate(EXTEND_CARTRIDGELIST_PROD, ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateCartridgeList.outputFile)
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                environmentTypes.set(PROD_ENVS)

                provideOutputFile(project.layout.buildDirectory.file(("${PROD_CONTAINER_FOLDER}/${CARTRIDGELIST_FOLDER}/${CARTRIDGELIST_FILENAME}")))
            }

            tasks.maybeCreate(EXTEND_CARTRIDGELIST_TEST, ExtendCartridgeList::class.java).apply {
                provideTemplateFile(templateCartridgeList.outputFile)
                provideCartridges(extension.projectConfig.cartridgesProvider)
                provideDBprepareCartridges(extension.projectConfig.dbprepareCartridgesProvider)

                environmentTypes.set(TEST_ENVS)

                provideOutputFile(project.layout.buildDirectory.file(("${TEST_CONTAINER_FOLDER}/${CARTRIDGELIST_FOLDER}/${CARTRIDGELIST_FILENAME}")))
            }

            tasks.maybeCreate(EXTEND_CARTRIDGELIST, ExtendCartridgeList::class.java).apply {
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

            val createSites = tasks.maybeCreate("runtest", CreateSitesFolder::class.java).apply {
                this.baseProject.set(extension.projectConfig.base)
                this.modules.set(extension.projectConfig.modules.asMap)

                this.baseFolderConfig.set(extension.projectConfig.folderConfig.base.sites)
                this.extraFolderConfig.set(extension.projectConfig.folderConfig.dev.sites)
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
        /**
        with(project) {
            val conf = extension.projectConfig.conf
            if (conf.dir != null) {
                var spec = project.copySpec()
                spec.from(conf.dir)

                conf.includes.forEach {
                    spec.include(it)
                }
                conf.excludes.forEach {
                    spec.exclude(it)
                }

                if(conf.targetPath != null) {
                    spec.into(conf.targetPath!!)
                }

                return tasks.maybeCreate("zipConf", org.gradle.api.tasks.bundling.Zip::class.java).apply {
                    this.with(spec)

                    this.includeEmptyDirs = false

                    this.archiveFileName.set("configuration.zip")
                    this.archiveClassifier.set("configuration")
                    this.destinationDirectory.set(project.layout.buildDirectory.dir("publish/configuration"))
                }
            }
        }
        **/
        return null
    }

    private fun configureSitesFolder(project: Project, extension: IntershopExtension) : Task? {
        /**
        with(project) {
            val conf = extension.projectConfig.sites
            if (conf.dir != null) {
                var spec = project.copySpec()
                spec.from(conf.dir)

                conf.includes.forEach {
                    spec.include(it)
                }
                conf.excludes.forEach {
                    spec.exclude(it)
                }

                if(conf.targetPath != null) {
                    spec.into(conf.targetPath!!)
                }

                return tasks.maybeCreate("zipSites", org.gradle.api.tasks.bundling.Zip::class.java).apply {
                    this.with(spec)

                    this.includeEmptyDirs = false

                    this.archiveFileName.set("sites.zip")
                    this.archiveClassifier.set("sites")
                    this.destinationDirectory.set(project.layout.buildDirectory.dir("publish/sites"))
                }
            }
        }
        **/
        return null
    }
}

