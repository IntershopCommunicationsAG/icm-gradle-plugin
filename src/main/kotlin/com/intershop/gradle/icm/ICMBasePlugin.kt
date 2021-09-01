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

import com.intershop.gradle.icm.cartridge.CartridgePlugin
import com.intershop.gradle.icm.cartridge.ContainerPlugin
import com.intershop.gradle.icm.cartridge.ProductPlugin
import com.intershop.gradle.icm.cartridge.TestPlugin
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.CollectLibraries
import com.intershop.gradle.icm.tasks.CreateClusterID
import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import com.intershop.gradle.isml.IsmlPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import java.io.FileFilter

/**
 * The base plugin for the configuration of the ICM project.
 */
open class ICMBasePlugin: Plugin<Project> {

    companion object {
        const val TASK_ALLDEPENDENCIESREPORT = "allDependencies"

        const val CONFIGURATION_CARTRIDGE = "cartridge"
        const val CONFIGURATION_CARTRIDGE_API = "cartridgeApi"
        const val CONFIGURATION_CARTRIDGE_RUNTIME = "cartridgeRuntime"

        /**
         * checks if the specified name is available in the list of tasks.
         *
         * @param taskname  the name of the new task
         * @param tasks     the task container self
         */
        fun checkForTask(tasks: TaskContainer, taskname: String): Boolean {
            return tasks.names.contains(taskname)
        }
    }

    override fun apply(project: Project) {
        with(project) {
            if (rootProject == this) {

                logger.info("ICM build plugin will be initialized")

                // apply maven publishing plugin to root and subprojects
                plugins.apply(MavenPublishPlugin::class.java)

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(
                        IntershopExtension.INTERSHOP_EXTENSION_NAME,
                        IntershopExtension::class.java,
                        project)

                plugins.withType(JavaPlugin::class.java) {
                    configureBaseConfigurations(this)
                }

                subprojects.forEach { prj ->
                    prj.plugins.withType(JavaPlugin::class.java) {
                        configureBaseConfigurations(prj)
                    }
                }

                configureClusterIdTask()
                configureCreateServerInfoPropertiesTask(extension)
                val configureCollectLibrariesTask = configureCollectLibrariesTask()

                if(! checkForTask(tasks, TASK_ALLDEPENDENCIESREPORT)) {
                    tasks.register(TASK_ALLDEPENDENCIESREPORT, DependencyReportTask::class.java)
                }

                createPackageTasks(this, configureCollectLibrariesTask)

            } else {
                logger.warn("ICM build plugin will be not applied to the sub project '{}'", name)
            }
        }
    }

    private fun configureBaseConfigurations(project: Project) {
        with(project.configurations) {
            val implementation = findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
            val api = findByName(JavaPlugin.API_CONFIGURATION_NAME)
            val runtimeOnly = findByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)

            val cartridge = maybeCreate(CONFIGURATION_CARTRIDGE)
            cartridge.description = "Cartridge Implementation only dependencies for source set 'main'."
            val cartridgeApi = maybeCreate(CONFIGURATION_CARTRIDGE_API)
            cartridgeApi.description = "Cartridge API dependencies for source set 'main'."
            val cartridgeRuntime = maybeCreate(CONFIGURATION_CARTRIDGE_RUNTIME)
            cartridgeRuntime.description = "Cartridge Runtime only dependencies for source set 'main'."

            implementation?.extendsFrom(cartridge)
            api?.extendsFrom(cartridgeApi)
            cartridgeApi.extendsFrom(cartridge)
            runtimeOnly?.extendsFrom(cartridgeRuntime)
            cartridgeRuntime.extendsFrom(cartridgeApi)
        }
    }

    private fun Project.configureCreateServerInfoPropertiesTask(extension: IntershopExtension) {
        tasks.register( CreateServerInfo.DEFAULT_NAME, CreateServerInfo::class.java ) { task ->
            task.provideProductId(extension.projectInfo.productIDProvider)
            task.provideProductName(extension.projectInfo.productNameProvider)
            task.provideCopyrightOwner(extension.projectInfo.copyrightOwnerProvider)
            task.provideCopyrightFrom(extension.projectInfo.copyrightFromProvider)
            task.provideOrganization(extension.projectInfo.organizationProvider)
        }
    }

    private fun Project.configureClusterIdTask() {
        tasks.register( CreateClusterID.DEFAULT_NAME, CreateClusterID::class.java )
    }

    private fun Project.configureCollectLibrariesTask() : TaskProvider<CollectLibraries> {
        val collectLibrariesTask = tasks.register(CollectLibraries.DEFAULT_NAME, CollectLibraries::class.java)
        subprojects { sub ->
            sub.plugins.withType(CartridgePlugin::class.java) {
                val writeCartridgeDescriptorTask = sub.tasks.named(WriteCartridgeDescriptor.DEFAULT_NAME, WriteCartridgeDescriptor::class.java)
                collectLibrariesTask.configure{ clt ->
                    clt.dependsOn(writeCartridgeDescriptorTask)
                }
            }
        }
        return collectLibrariesTask
    }

    private fun Project.createPackageTasks(project: Project, configureCollectLibrariesTask: TaskProvider<CollectLibraries>) {
        val createMainPackage = tasks.register(CreateMainPackage.DEFAULT_NAME, CreateMainPackage::class.java)
        val createTestPackage = tasks.register(CreateTestPackage.DEFAULT_NAME, CreateTestPackage::class.java)

        subprojects {sub ->
            sub.plugins.withType(CartridgePlugin::class.java) {
                val cartridgefiles = project.copySpec { cp ->
                    if(sub.layout.projectDirectory.dir("staticfiles/cartridge").asFile.exists()) {
                        cp.from(sub.layout.projectDirectory.dir("staticfiles/cartridge")) { cps ->
                            intoRelease(cps, sub)
                        }
                    }

                    sub.plugins.withType(IsmlPlugin::class.java) {
                        cp.from(sub.tasks.getByName("isml2classMain")) { cpt ->
                            intoRelease(cpt, sub)
                        }
                    }

                    sub.plugins.withType(JavaPlugin::class.java) {
                        cp.from(sub.tasks.getByName("jar")) { cps ->
                            cps.into("cartridges/${sub.name}/release/lib")
                        }
                    }
                }

                sub.plugins.withType(ProductPlugin::class.java) {
                    sub.plugins.withType(IsmlPlugin::class.java) {
                        createMainPackage.configure { mainpkg -> pkgDependsOn(mainpkg, sub) }
                    }
                    createMainPackage.configure { mainpkg -> mainpkg.with(cartridgefiles) }
                }

                sub.plugins.withType(ContainerPlugin::class.java) {
                    sub.plugins.withType(IsmlPlugin::class.java) {
                        createMainPackage.configure { mainpkg -> pkgDependsOn(mainpkg, sub) }
                    }
                    createMainPackage.configure { mainpkg -> mainpkg.with(cartridgefiles) }
                }

                sub.plugins.withType(TestPlugin::class.java) {
                    sub.plugins.withType(IsmlPlugin::class.java) {
                        createTestPackage.configure { testpkg -> pkgDependsOn(testpkg, sub) }
                    }
                    createTestPackage.configure { testpkg -> testpkg.with(cartridgefiles) }
                }
            }
        }

        createMainPackage.configure {
            it.dependsOn(configureCollectLibrariesTask)
            it.with(configureCollectLibrariesTask.get().copySpecFor(EnvironmentType.PRODUCTION))
        }
        createTestPackage.configure {
            it.dependsOn(configureCollectLibrariesTask)
            it.with(configureCollectLibrariesTask.get().copySpecFor(EnvironmentType.PRODUCTION))
            it.with(configureCollectLibrariesTask.get().copySpecFor(EnvironmentType.TEST))
        }
    }

    private fun intoRelease(cpsp: CopySpec, prj: Project) = cpsp.into("cartridges/${prj.name}/release")

    private fun pkgDependsOn(tar: Tar, prj: Project) {
        tar.dependsOn(prj.tasks.named("isml2classMain"))
        tar.dependsOn(prj.tasks.named("jar"))
    }
}
