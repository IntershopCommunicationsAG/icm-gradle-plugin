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
import com.intershop.gradle.icm.tasks.CopyLibraries
import com.intershop.gradle.icm.tasks.CreateLibList
import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.diagnostics.DependencyReportTask

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
                plugins.apply(BasePlugin::class.java)

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(
                        IntershopExtension.INTERSHOP_EXTENSION_NAME,
                        IntershopExtension::class.java,
                        project)

                plugins.withType(JavaPlugin::class.java) {
                    configureBaseConfigurations(this)
                }

                val rootAssembleTask = tasks.named(BasePlugin.ASSEMBLE_TASK_NAME)
                subprojects.forEach { subProject ->
                    subProject.plugins.withType(JavaPlugin::class.java) {
                        configureBaseConfigurations(subProject)
                    }
                    // enforce: root.assemble dependsOn <allSubprojectsWithBasePlugin>.assemble
                    subProject.plugins.withType(BasePlugin::class.java) {
                        rootAssembleTask.configure { rootAssemble ->
                            rootAssemble.dependsOn(subProject.tasks.named(BasePlugin.ASSEMBLE_TASK_NAME))
                        }
                    }
                }

                val createMainPackage = tasks.register(CreateMainPackage.DEFAULT_NAME, CreateMainPackage::class.java)
                val createTestPackage = tasks.register(CreateTestPackage.DEFAULT_NAME, CreateTestPackage::class.java)

                configureCreateServerInfoPropertiesTask(extension)
                configureCollectLibrariesTask(createMainPackage, createTestPackage)
                configurePackageTasks(this, createMainPackage, createTestPackage)

                if(! checkForTask(tasks, TASK_ALLDEPENDENCIESREPORT)) {
                    tasks.register(TASK_ALLDEPENDENCIESREPORT, DependencyReportTask::class.java)
                }

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

    private fun Project.configureCollectLibrariesTask(createMainPackage: TaskProvider<CreateMainPackage>,
                                                      createTestPackage: TaskProvider<CreateTestPackage>) {
        val collectLibraries = tasks.register("collectLibraries")

        val prodLibListTask = registerLibListTask(EnvironmentType.PRODUCTION, packageUsing = createMainPackage)
        val prodLibCopyTask = registerLibCopyTask(EnvironmentType.PRODUCTION, prodLibListTask, createMainPackage)

        val testLibListTask = registerLibListTask(EnvironmentType.TEST, packageUsing = createTestPackage, prodLibListTask)
        val testLibCopyTask = registerLibCopyTask(EnvironmentType.TEST, testLibListTask, packageUsing = createTestPackage)

        val devLibListTask = registerLibListTask(EnvironmentType.DEVELOPMENT, packageUsing = null /* not packaged */, prodLibListTask, testLibListTask)
        val devLibCopyTask = registerLibCopyTask(EnvironmentType.DEVELOPMENT, devLibListTask, packageUsing = null /* not packaged */)

        collectLibraries.configure {
            it.dependsOn(prodLibCopyTask, testLibCopyTask, devLibCopyTask)
        }

        subprojects { sub ->
            sub.plugins.withType(CartridgePlugin::class.java) {
                val writeCartridgeDescriptorTask = sub.tasks.
                    named(WriteCartridgeDescriptor.DEFAULT_NAME, WriteCartridgeDescriptor::class.java)

                when(CartridgeUtil.getCartridgeStyle(sub).environmentType()) {
                    EnvironmentType.PRODUCTION ->
                        configureLibListTask(prodLibListTask, writeCartridgeDescriptorTask)
                    EnvironmentType.TEST ->
                        configureLibListTask(testLibListTask, writeCartridgeDescriptorTask)
                    EnvironmentType.DEVELOPMENT ->
                        configureLibListTask(devLibListTask, writeCartridgeDescriptorTask)
                    EnvironmentType.ALL -> {
                        configureLibListTask(prodLibListTask, writeCartridgeDescriptorTask)
                        configureLibListTask(testLibListTask, writeCartridgeDescriptorTask)
                    }
                }
            }
        }
    }

    private fun Project.registerLibListTask(type: EnvironmentType, packageUsing : TaskProvider<out Tar>? = null, vararg basedOn : TaskProvider<CreateLibList>): TaskProvider<CreateLibList> {
        val libList = tasks.register(CreateLibList.getName(type.toString()), CreateLibList::class.java) { cll ->
            cll.environmentType.set(type)
            cll.libraryListFile.set(project.layout.buildDirectory.file(CreateLibList.getOutputPath(type.name)))
            cll.dependsOn(basedOn)
            basedOn.forEach { bo ->
                cll.basedOnLibraryLists.add(bo.get().libraryListFile)
            }
        }
        packageUsing?.configure { pkg ->
            pkg.dependsOn(libList)
            pkg.from(libList.get().outputs) { copySpec ->
                copySpec.into("lib")
            }
        }
        return libList
    }

    private fun Project.registerLibCopyTask(type: EnvironmentType,
                                            cll: TaskProvider<CreateLibList>, packageUsing : TaskProvider<out Tar>? = null): TaskProvider<CopyLibraries> {
        val copyLibraries = tasks.register(CopyLibraries.getName(type.toString()), CopyLibraries::class.java) { cl ->
            cl.environmentType.set(type.name)
            cl.librariesDirectory.set(project.layout.buildDirectory.dir(CopyLibraries.getOutputPath(type.name)))
            cl.dependencyIDFile.set(cll.get().libraryListFile)
            cl.dependsOn(cll)
        }
        packageUsing?.configure { pkg ->
            pkg.dependsOn(copyLibraries)
            pkg.from(copyLibraries.get().librariesDirectory) { copySpec ->
                copySpec.into("lib")
            }
        }
        return copyLibraries
    }
    private fun configureLibListTask(clt: TaskProvider<CreateLibList>,
                                     wcd: TaskProvider<WriteCartridgeDescriptor>) {
        clt.configure {
            it.dependsOn(wcd)
            it.cartridgeDescriptors.add(wcd.get().outputFile)
        }
    }

    private fun Project.configurePackageTasks(project: Project,
                                              createMainPackage: TaskProvider<CreateMainPackage>,
                                              createTestPackage: TaskProvider<CreateTestPackage>) {
        subprojects {sub ->
            sub.plugins.withType(CartridgePlugin::class.java) {
                val cartridgefiles = project.copySpec { cp ->
                    if(sub.layout.projectDirectory.dir("staticfiles/cartridge").asFile.exists()) {
                        cp.from(sub.layout.projectDirectory.dir("staticfiles/cartridge")) { cps ->
                            intoRelease(cps, sub)
                        }
                    }

                    sub.plugins.withType(JavaPlugin::class.java) {
                        cp.from(sub.tasks.getByName("jar")) { cps ->
                            cps.into("cartridges/${sub.name}/release/lib")
                        }
                    }
                }

                sub.plugins.withType(ProductPlugin::class.java) {
                    createMainPackage.configure { mainpkg -> mainpkg.with(cartridgefiles) }
                }

                sub.plugins.withType(ContainerPlugin::class.java) {
                    createMainPackage.configure { mainpkg -> mainpkg.with(cartridgefiles) }
                }

                sub.plugins.withType(TestPlugin::class.java) {
                    createTestPackage.configure { testpkg -> testpkg.with(cartridgefiles) }
                }
            }
        }
    }

    private fun intoRelease(cpsp: CopySpec, prj: Project) = cpsp.into("cartridges/${prj.name}/release")

}
