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
import com.intershop.gradle.icm.tasks.CreateServerDirProperties
import com.intershop.gradle.icm.tasks.DBInit
import com.intershop.gradle.icm.utils.OsCheck
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

/**
 * The plugin for the configuration of the ICM base project.
 */
class ICMProductPlugin : Plugin<Project> {

    private lateinit var runtimeLibConfiguration: Configuration

    companion object {
        const val SERVER_DIRECTORY_PROPERTIES_DIR = "serverconfig"
        const val SERVER_DIRECTORY_PROPERTIES = "serverdir.properties"

        // ICM base project
        // ... configurations
        const val CONFIGURATION_DBINIT = "dbinit"
        const val CONFIGURATION_ICMSERVER = "icmserver"

        const val CONFIGURATION_RUNTIME_LIB = "runtimeLib"
        const val CONFIGURATION_DOCKER_RUNTIME_LIB = "dockerRuntimeLib"

        // ... parameters
        const val PARAMETER_LOCALCONFIGURATION = "localConfig"

        // ... tasks
        const val TASK_INSTALLRUNTIMELIB = "installRuntimeLib"
        const val TASK_ISHUNIT_PARALLEL = "ishUnitTestParallel"
        const val TASK_ISHUNIT_SERIAL = "ishUnitTestSerial"
        const val TASK_ISHUNIT = "ishUnitTest"
    }

    override fun apply(project: Project) {
        with(project) {
            if(project.rootProject == this) {

                // base plugin must be applied
                if(plugins.findPlugin(ICMBasePlugin::class.java) == null) {
                    plugins.apply(ICMBasePlugin::class.java)
                }

                val extension = extensions.getByType(IntershopExtension::class.java)

                // create configurations for ICM project
                configurations.maybeCreate(CONFIGURATION_DBINIT)
                    .setTransitive(false).description = "Configuration for dbinit execution of the ICM base project"

                configurations.maybeCreate(CONFIGURATION_ICMSERVER)
                    .setTransitive(false).description = "Configuration for ICM server execution of the ICM base project"

                addRuntimeDependencies(this, extension)
                addInstallRuntimeLib(this)

                if (!ICMBasePlugin.checkForTask(tasks, CreateServerDirProperties.DEFAULT_NAME)) {
                    tasks.register(
                        CreateServerDirProperties.DEFAULT_NAME,
                        CreateServerDirProperties::class.java
                    ) {

                        val configFolderTask = tasks.getByPath(extension.baseConfig.configurationFolderTaskPath)
                        val sitesFolderTask = tasks.getByPath(extension.baseConfig.sitesFolderTaskPath)

                        it.dependsOn(configFolderTask, sitesFolderTask)

                        it.addSource(projectDir.absolutePath)
                        this.subprojects.forEach { subprj ->
                            if(subprj.subprojects.size > 0) {
                                if(! File(subprj.projectDir, ".noCartridges").exists()) {
                                    it.addSource(subprj.projectDir.absolutePath)
                                }
                            }
                        }

                        it.configDir = configFolderTask.outputs.files.singleFile.absolutePath
                        it.sitesDir = sitesFolderTask.outputs.files.singleFile.absolutePath
                    }
                }

                if (!ICMBasePlugin.checkForTask(tasks, DBInit.DEFAULT_NAME)) {
                    tasks.register(
                        DBInit.DEFAULT_NAME,
                        DBInit::class.java
                    ) {
                        it.dependsOn(rootProject.tasks.getByName(TASK_WRITECARTRIDGEFILES))
                    }
                }

                if (!ICMBasePlugin.checkForTask(tasks, TASK_ISHUNIT)) {
                    project.plugins.withType(LifecycleBasePlugin::class.java) {

                        val checkTask = tasks.findByName(CHECK_TASK_NAME)

                        val ishUnitTask = tasks.register(TASK_ISHUNIT) {
                            it.description = "Starts all ISHUnit tests"
                        }

                        checkTask?.dependsOn(ishUnitTask)

                        if (!ICMBasePlugin.checkForTask(tasks, TASK_ISHUNIT_PARALLEL)) {
                            val ishUnitParallel = tasks.register(TASK_ISHUNIT_PARALLEL) {
                                it.description = "Starts all ISHUnit tests in different projects parallel"
                            }
                            ishUnitTask.get().dependsOn(ishUnitParallel.get())
                        }
                        if (!ICMBasePlugin.checkForTask(tasks, TASK_ISHUNIT_SERIAL)) {
                            val ishUnitSerial = tasks.register(TASK_ISHUNIT_SERIAL) {
                                it.description = "Starts one test for serial execution"
                            }
                            ishUnitTask.get().dependsOn(ishUnitSerial.get())
                        }
                    }
                }

            }
        }
    }

    private fun addRuntimeDependencies(project: Project, extension: IntershopExtension) {
        val dependencyHandler = project.dependencies

        runtimeLibConfiguration = project.configurations.maybeCreate(CONFIGURATION_RUNTIME_LIB)
            .setTransitive(false)
            .setDescription("Configuration for native runtime library")
            .defaultDependencies {
                val dependencyBase = "${extension.baseConfig.runtimeModule}:${extension.baseConfig.runtimeVersion}"

                if(OsCheck.getDetectedOS() == OsCheck.OSType.MacOS) {
                    it.add( dependencyHandler.create("${dependencyBase}:darwin@dylib") )
                    it.add( dependencyHandler.create("${dependencyBase}:darwin@setpgid") )
                }
                if(OsCheck.getDetectedOS() == OsCheck.OSType.Linux) {
                    it.add( dependencyHandler.create("${dependencyBase}:linux@so") )
                    it.add( dependencyHandler.create("${dependencyBase}:linux@setpgid") )
                }
                if(OsCheck.getDetectedOS() == OsCheck.OSType.Windows) {
                    it.add( dependencyHandler.create("${dependencyBase}:win32@dll") )
                }
            }

        project.configurations.maybeCreate(CONFIGURATION_DOCKER_RUNTIME_LIB)
            .setTransitive(false)
            .setDescription("Configuration for native runtime library used with Docker")
            .defaultDependencies {
                val dependencyBase = "${extension.baseConfig.runtimeModule}:${extension.baseConfig.runtimeVersion}"


                it.add( dependencyHandler.create("${dependencyBase}:darwin@dylib") )
                it.add( dependencyHandler.create("${dependencyBase}:darwin@setpgid") )
            }
    }

    private fun addInstallRuntimeLib(project: Project) {
        with(project) {
            if(! ICMBasePlugin.checkForTask(tasks, TASK_INSTALLRUNTIMELIB)) {
                tasks.register(TASK_INSTALLRUNTIMELIB, Copy::class.java) { cp ->
                    cp.from(runtimeLibConfiguration)

                    cp.rename("(.*).dll", "ish-runtime.dll")
                    cp.rename("(.*).so", "libish-runtime.so")
                    cp.rename("(.*).dylib", "libish-runtime.dylib")
                    cp.rename("(.*).setpgid", "setpgid")

                    cp.into(File(buildDir, "runtime-lib"))
                }
            }
        }
    }


}
