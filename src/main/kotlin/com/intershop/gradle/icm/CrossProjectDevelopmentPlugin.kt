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
import com.intershop.gradle.icm.tasks.crossproject.IncludedBuild
import com.intershop.gradle.icm.tasks.crossproject.PrepareConfigFolder
import com.intershop.gradle.icm.tasks.crossproject.WriteMappingFile
import com.intershop.gradle.icm.utils.CopySpecUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.GradleException
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

        const val TASK_PREPAREPRJ = "prepareCrossProject"

        const val TASK_WRITEMAPPINGFILES = "writeMappingFiles"
        const val TASK_PREPARE_CONFIG = "prepareCrossProjectConfig"
        const val TASK_PREPARE_CARTRIDGELIST = "prepareCrossProjectCartridgeList"

        const val CROSSPRJ_BUILD_DIR = "combinedbuild"

        const val CROSSPRJ_MODULES = "cross.project.modules"
        const val CROSSPRJ_FINALPROJECT = "cross.project.finalproject"
        const val CROSSPRJ_BASEPROJECT = "cross.project.baseproject"

        const val CROSSPRJ_PATH = "cross.project.path"
    }

    override fun apply(project: Project) {
        with(project) {
            if (project.rootProject == this) {
                logger.info("ICM Cross-Project Development plugin will be initialized")

                val intershopExtension = extensions.getByType(IntershopExtension::class.java)
                val developmentConfig = intershopExtension.developmentConfig
                val projectConfig = intershopExtension.projectConfig

                this.tasks.register(TASK_WRITEMAPPINGFILES, WriteMappingFile::class.java).configure {
                    it.group = TASK_GROUP
                    it.description = "Writes mapping files like settings.gradle.kts file for composite builds"
                }

                val modulesStr = developmentConfig.getConfigProperty(CROSSPRJ_MODULES)
                val finalProjectName = developmentConfig.getConfigProperty(CROSSPRJ_FINALPROJECT)

                val modules = mutableMapOf<String, IncludedBuild>()
                if(modulesStr.isNotBlank()) {
                    val ml = modulesStr.split(";")
                    ml.forEach {
                        val ib = it.split(":")
                        if(ib.size > 1) {
                            modules.put(ib[0], IncludedBuild(ib[0], ib[1]))
                        } else {
                            throw GradleException("Check your development configuration! (" + CROSSPRJ_MODULES + ").")
                        }
                    }
                }

                val crossProjPath = developmentConfig.getConfigProperty(CROSSPRJ_PATH)

                if(modules.keys.contains(project.name)) {
                    prepareModulesTasks(this, crossProjPath, projectConfig)
                }

                if(project.name == finalProjectName) {
                    prepareStoreFrontTasks(this, crossProjPath, projectConfig, modules)
                }


            }
        }
    }

    private fun prepareModulesTasks(project: Project, projectConfig: ProjectConfiguration) {
        with(project) {
            val configCopySpec =
                CopySpecUtil.getCSForServerDir(this, projectConfig.serverDirConfig.base)

            val crossPrjConf = tasks.register(TASK_PREPARE_CONFIG, Copy::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all module files for conf folder"

                it.with(configCopySpec)
                it.into(File(projectDir, "${CROSSPRJ_FOLDERPATH}/${name}/conf"))
            }

            tasks.register(TASK_PREPAREPRJ).configure {
                it.group = TASK_GROUP
                it.description = "Start all copy tasks for modules"

                it.dependsOn(crossPrjConf)
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

            val baseDir = File(projectDir, CROSSPRJ_FOLDERPATH)

            val moduleConfDirs = mutableMapOf<String, File>()
            modules.forEach { module ->
                val dep = confprops[module].toString()
                if(dep.isNotBlank()) {
                    moduleConfDirs[dep] = File(baseDir, "${module}/conf")
                }
            }

            // create mapping dependency -> filesystem
            val mainPrj = confprops.getProperty("mainproject", "")

            val versionInfoTask = tasks.named(CreateServerInfo.DEFAULT_NAME, CreateServerInfo::class.java)

            val crossPrjConf = tasks.register(TASK_PREPAREPRJ_CONFIG, PrepareConfigFolder::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all files for server conf folder for storefront project"

                project.provider { prepareCartridgeList.get().outputFile.get() }
                it.baseProject.set(projectConfig.base)
                it.baseDirConfig.set(projectConfig.serverDirConfig.base)
                it.extraDirConfig.set(projectConfig.serverDirConfig.
                getServerDir(EnvironmentType.DEVELOPMENT))
                it.mainBaseDir.set(File(baseDir, "${mainPrj}/${CROSSPRJ_CONF}"))

                projectConfig.modules.all { ncp ->
                    it.module(ncp)
                }

                it.moduleDirectories.set(moduleConfDirs)
                it.provideCartridgeListFile( project.provider { prepareCartridgeList.get().outputFile.get() } )
                it.provideVersionInfoFile( project.provider { versionInfoTask.get().outputFile.get() } )

                it.dependsOn(prepareCartridgeList, versionInfoTask)
            }

            tasks.register(TASK_PREPAREPRJ).configure {
                it.group = TASK_GROUP
                it.description = "Copy all files for server folder for storefront project"

                it.dependsOn(crossPrjConf)
            }
        }
    }
}
