/*
 * Copyright 2021 Intershop Communications AG.
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
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.ExtendCartridgeList
import com.intershop.gradle.icm.tasks.crossproject.PrepareConfigFolder
import com.intershop.gradle.icm.tasks.crossproject.PrepareSitesFolder
import com.intershop.gradle.icm.tasks.crossproject.WriteMappingFile
import com.intershop.gradle.icm.utils.CopySpecUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File
import java.util.Properties

/**
 * Special plugin for Intershop internal development.
 */
class CrossProjectDevelopmentPlugin: Plugin<Project> {

    companion object {
        const val TASK_GROUP = "ICM Cross-Project Development"
        const val CROSSPRJ_PATH = "../icm-cross-project"
        const val CROSSPRJ_PROPERTIES = "crossprjconfig.properties"
        const val CROSSPRJ_FOLDER = "folder"
        const val CROSSPRJ_CONF = "conf"

        const val CROSSPRJ_FOLDERPATH = "${CROSSPRJ_PATH}/${CROSSPRJ_FOLDER}"
        const val CROSSPRJ_CONFPATH = "${CROSSPRJ_PATH}/${CROSSPRJ_CONF}"

        const val TASK_WRITEMAPPINGFILES = "writeMappingFiles"

        const val TASK_PREPAREPRJ_SITES = "prepareCrossProjectSites"
        const val TASK_PREPAREPRJ_CONFIG = "prepareCrossProjectConfig"
        const val TASK_PREPAREPRJ = "prepareCrossProject"
        const val TASK_PREPARE_CARTRIDGELIST = "prepareCrossCartridgeList"
    }

    override fun apply(project: Project) {
        with(project) {
            if (project.rootProject == this) {
                logger.info("ICM Cross-Project Development plugin will be initialized")

                val projectConfig = extensions.getByType(IntershopExtension::class.java).projectConfig

                this.tasks.register(TASK_WRITEMAPPINGFILES, WriteMappingFile::class.java).configure {
                    it.group = TASK_GROUP
                    it.description = "Writes mapping files like settings.gradle.kts file for composite builds"
                }

                // read configuration file from project
                val confFile = File(projectDir, "${CROSSPRJ_CONFPATH}/${CROSSPRJ_PROPERTIES}")
                val confprops = Properties()
                if(confFile.exists()) {
                    confprops.load(confFile.inputStream())
                }

                val modulesStr = confprops.getProperty( "modules", "")
                val modules = mutableListOf<String>()
                if(modulesStr.isNotBlank()) {
                    val ml = modulesStr.split(";")
                    ml.forEach {
                        modules.add(it.trim())
                    }
                }

                if(modules.contains(project.name)) {
                    prepareModulesTasks(this, projectConfig)
                }

                if(project.name == confprops.getProperty("storefrontproject")) {
                    prepareStoreFrontTasks(this, projectConfig, confprops, modules)
                }
            }
        }
    }

    private fun prepareModulesTasks(project: Project, projectConfig: ProjectConfiguration) {
        with(project) {
            val sitesCopySpec =
                CopySpecUtil.getCSForServerDir(this, projectConfig.serverDirConfig.base.sites)
            val configCopySpec =
                CopySpecUtil.getCSForServerDir(this, projectConfig.serverDirConfig.base.config)

            val crossPrjSites = tasks.register(TASK_PREPAREPRJ_SITES, Copy::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all module files for sites folder"

                it.with(sitesCopySpec)
                it.into(File(projectDir, "${CROSSPRJ_FOLDERPATH}/${name}/sites"))
            }

            val crossPrjConf = tasks.register(TASK_PREPAREPRJ_CONFIG, Copy::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all module files for conf folder"

                it.with(configCopySpec)
                it.into(File(projectDir, "${CROSSPRJ_FOLDERPATH}/${name}/conf"))
            }

            tasks.register(TASK_PREPAREPRJ).configure {
                it.group = TASK_GROUP
                it.description = "Start all copy tasks for modules"

                it.dependsOn(crossPrjConf, crossPrjSites)
            }
        }
    }

    private fun prepareStoreFrontTasks(project: Project, projectConfig: ProjectConfiguration,
                                       confprops: Properties, modules: List<String>) {
        with(project) {
            val cartridgelistFile = confprops.getProperty("cartridgelist", "")
            if(cartridgelistFile.isBlank()) {
                this.logger.error(
                    "There is currently no configuration for the" +
                            "cartridges list properties file template!"
                )
            }
            val templateFile = File(cartridgelistFile)

            val prepareCartridgeList = tasks.register(TASK_PREPARE_CARTRIDGELIST,
                        ExtendCartridgeList::class.java) {
                it.group = TASK_GROUP
                it.description = "Generates an cartridgelist.properties file for cross project development"

                it.templateFile.set(templateFile)

                it.provideCartridges(projectConfig.cartridges)
                it.provideDBprepareCartridges(projectConfig.dbprepareCartridges)
                it.environmentTypes.set(ICMProjectPlugin.DEVELOPMENT_ENVS)

                it.outputFile.set(File(projectDir,
                    "${CROSSPRJ_CONFPATH}/cartridgelist.properties"))
            }

            val baseDir = File(projectDir, ".${CROSSPRJ_FOLDERPATH}")

            val moduleConfDirs = mutableMapOf<String, File>()
            val moduleSiteDirs = mutableMapOf<String, File>()
            modules.forEach { module ->
                val dep = confprops[module].toString()
                if(dep.isNotBlank()) {
                    moduleConfDirs[dep] = File(baseDir, "${module}/conf")
                    moduleSiteDirs[dep] = File(baseDir, "${module}/sites")
                }
            }

            // create mapping dependency -> filesystem
            val mainPrj = confprops.getProperty("mainproject", "")

            val crossPrjSites = tasks.register(TASK_PREPAREPRJ_SITES, PrepareSitesFolder::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all files for server sites folder for storefront project"

                it.baseProject.set(projectConfig.base)
                it.baseDirConfig.set(projectConfig.serverDirConfig.base.sites)
                it.extraDirConfig.set(
                    projectConfig.serverDirConfig.getServerDirSet(EnvironmentType.DEVELOPMENT).sites)
                it.mainBaseDir.set(File(baseDir, "${mainPrj}/sites"))
                it.moduleDirectories.set(moduleSiteDirs)
            }

            val versionInfoTask = tasks.named(CreateServerInfo.DEFAULT_NAME, CreateServerInfo::class.java)

            val crossPrjConf = tasks.register(TASK_PREPAREPRJ_CONFIG, PrepareConfigFolder::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all files for server conf folder for storefront project"

                project.provider { prepareCartridgeList.get().outputFile.get() }
                it.baseProject.set(projectConfig.base)
                it.baseDirConfig.set(projectConfig.serverDirConfig.base.config)
                it.extraDirConfig.set(projectConfig.serverDirConfig.
                getServerDirSet(EnvironmentType.DEVELOPMENT).config)
                it.mainBaseDir.set(File(baseDir, "${mainPrj}/${CROSSPRJ_CONF}"))
                it.moduleDirectories.set(moduleConfDirs)
                it.provideCartridgeListFile( project.provider { prepareCartridgeList.get().outputFile.get() } )
                it.provideVersionInfoFile( project.provider { versionInfoTask.get().outputFile.get() } )

                it.dependsOn(prepareCartridgeList, versionInfoTask)
            }

            tasks.register(TASK_PREPAREPRJ).configure {
                it.group = TASK_GROUP
                it.description = "Copy all files for server folder for storefront project"

                it.dependsOn(crossPrjConf, crossPrjSites)
            }
        }
    }
}
