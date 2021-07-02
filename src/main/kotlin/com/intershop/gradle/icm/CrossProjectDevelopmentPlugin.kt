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

/**
 * Special plugin for Intershop internal development.
 */
class CrossProjectDevelopmentPlugin: Plugin<Project> {

    companion object {
        const val TASK_GROUP = "ICM Cross-Project Development"

        const val TASK_WRITEMAPPINGFILES = "writeMappingFiles"
        const val TASK_PREPARE_CONFIG = "prepareCrossProject"
        const val TASK_PREPARE_CARTRIDGELIST = "prepareCrossProjectCartridgeList"

        const val CROSSPRJ_BUILD_DIR = "compositebuild"

        const val CROSSPRJ_MODULES = "cross.project.modules"
        const val CROSSPRJ_FINALPROJECT = "cross.project.finalproject"
        const val CROSSPRJ_CARTRIDGELISTPATH = "cross.project.cartridgelist.path"

        const val CROSSPRJ_BASEPROJECT = "cross.project.baseproject"

        const val CROSSPRJ_CARTRIDGELISTPATH_DEFVALUE = "icm_config/cartridgelist/cartridgelist.properties"
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

                val modulesStr = developmentConfig.getConfigProperty(CROSSPRJ_MODULES, "")
                val finalProjectStr = developmentConfig.getConfigProperty(CROSSPRJ_FINALPROJECT, "")
                val baseProjectStr = developmentConfig.getConfigProperty(CROSSPRJ_BASEPROJECT, "")
                val cartridgeListPath = developmentConfig.getConfigProperty(
                    CROSSPRJ_CARTRIDGELISTPATH,
                    CROSSPRJ_CARTRIDGELISTPATH_DEFVALUE)

                var finalPrj: IncludedBuild?  = null

                val finalProjectList = finalProjectStr.split(":")
                if(finalProjectList.size == 2){
                    finalPrj = IncludedBuild(finalProjectList[0], finalProjectList[1])
                }

                var basePrj: IncludedBuild?  = null

                val baseProjectList = baseProjectStr.split(":")
                if(baseProjectList.size == 2){
                    basePrj = IncludedBuild(baseProjectList[0], baseProjectList[1])
                }

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

                if(modules.keys.contains(project.name)) {
                    prepareModulesTasks(this, projectConfig, basePrj)
                }

                if(project.name == finalPrj?.projectName) {
                    prepareStoreFrontTasks(this, projectConfig, cartridgeListPath, basePrj)
                }
            }
        }
    }

    private fun prepareModulesTasks(project: Project,
                                    projectConfig: ProjectConfiguration,
                                    basePrj: IncludedBuild?) {
        with(project) {
            val configCopySpec =
                CopySpecUtil.getCSForServerDir(this, projectConfig.serverDirConfig.base)

            tasks.register(TASK_PREPARE_CONFIG, Copy::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all module files for conf folder"

                it.with(configCopySpec)
                it.into(File("${basePrj?.projectPath}/build/compositeserver/${name}/conf"))
            }
        }
    }

    private fun prepareStoreFrontTasks(project: Project,
                                       projectConfig: ProjectConfiguration,
                                       cartridgelistPath: String,
                                       basePrj: IncludedBuild?) {
        with(project) {

            val templateFile = File("${basePrj?.projectPath}/${cartridgelistPath}")
            if(! templateFile.exists()) {
                logger.error(
                    "There is currently no configuration for the " +
                            "cartridges list properties file template! ({})", templateFile
                )
            }

            val prepareCartridgeList = tasks.register(TASK_PREPARE_CARTRIDGELIST,
                        ExtendCartridgeList::class.java) {
                it.group = TASK_GROUP
                it.description = "Generates an cartridgelist.properties file for cross project development"

                it.templateFile.set(templateFile)

                it.provideCartridges(projectConfig.cartridges)
                it.provideDBprepareCartridges(projectConfig.dbprepareCartridges)
                it.environmentTypes.set(ICMProjectPlugin.DEVELOPMENT_ENVS)

                it.outputFile.set(
                    File(
                        "${basePrj?.projectPath}/build/compositeserver/${name}/conf/cartridgelist.properties"))
            }

            val versionInfoTask = tasks.named(CreateServerInfo.DEFAULT_NAME, CreateServerInfo::class.java)

            tasks.register(TASK_PREPARE_CONFIG, PrepareConfigFolder::class.java) {
                it.group = TASK_GROUP
                it.description = "Copy all files for server conf folder for storefront project"

                project.provider { prepareCartridgeList.get().outputFile.get() }
                it.baseProject.set(projectConfig.base)
                it.baseDirConfig.set(projectConfig.serverDirConfig.base)
                it.extraDirConfig.set(projectConfig.serverDirConfig.
                getServerDir(EnvironmentType.DEVELOPMENT))
                it.mainBaseDir.set(File("${basePrj?.projectPath}/build/compositeserver/${basePrj?.projectName}/conf"))

                it.outputDir.set(File("${basePrj?.projectPath}/build/compositeserver/server/conf"))
                projectConfig.modules.all { ncp ->
                    it.module(ncp)
                }

                it.moduleDirectories.set(mutableMapOf<String, File>())
                it.provideCartridgeListFile( project.provider { prepareCartridgeList.get().outputFile.get() } )
                it.provideVersionInfoFile( project.provider { versionInfoTask.get().outputFile.get() } )

                it.dependsOn(prepareCartridgeList, versionInfoTask)
            }
        }
    }
}
