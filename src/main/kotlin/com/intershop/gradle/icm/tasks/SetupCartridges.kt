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

package com.intershop.gradle.icm.tasks

import com.intershop.gradle.icm.ICMProjectPlugin.Companion.CONFIGURATION_EXTERNALCARTRIDGES
import com.intershop.gradle.icm.extension.BaseProjectConfiguration
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.CartridgeUtil.downloadLibFilter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import java.io.File
import javax.inject.Inject

/**
 * Task for setup of an external cartridge in
 * an ICM project.
 */
open class SetupCartridges @Inject constructor(
        projectLayout: ProjectLayout,
        objectFactory: ObjectFactory,
        private var fsOps: FileSystemOperations) : DefaultTask() {

    @get:Internal
    val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()
    private val baseProjectsProperty: MapProperty<String, BaseProjectConfiguration> =
        objectFactory.mapProperty(String::class.java, BaseProjectConfiguration::class.java)
    private val productionCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Create a directory with external cartridges."

        outputDirProperty.convention(projectLayout.buildDirectory.dir("server/cartridges"))
    }

    @get:Nested
    var baseProjects: Map<String, BaseProjectConfiguration>
        get() = baseProjectsProperty.get()
        set(value) = baseProjectsProperty.putAll(value)

    /**
     * Filter for cartridges and dbprepareCartridges. All listed cartridges are production cartridges.
     * Not listed cartridges are test or development cartridges. If the list is empty all cartrdiges
     * are production catridges.
     *
     * @property productionCartridges cartridge list of production cartridges
     */
    @get:Optional
    @get:Input
    var productionCartridges: Set<String>
        get() = if(productionCartridgesProperty.orNull != null) productionCartridgesProperty.get() else setOf()
        set(value) = productionCartridgesProperty.set(value)

    /**
     * Provider to configure production cartridge filter.
     *
     * @param cartridges cartridge list
     */
    fun provideProductionCartridges(cartridges: Provider<Set<String>>) = productionCartridgesProperty.set(cartridges)

    /**
     * Configuration of external dependencies.
     *
     * @property externalCartridgeDependencies
     */
    @get:Input
    val externalCartridgeDependencies: List<String> by lazy {
        val returnDeps = mutableListOf<String>()
        project.configurations.getByName(CONFIGURATION_EXTERNALCARTRIDGES).dependencies.forEach {
            returnDeps.add(it.toString())
        }
        returnDeps
    }

    /**
     * Provider configuration for target directory.
     *
     * @param cartridgeDir
     */
    fun provideCartridgeDir(cartridgeDir: Provider<Directory>) = outputDirProperty.set(cartridgeDir)

    /**
     * Output directory of this task.
     *
     * @property cartridgeDir
     */
    @get:OutputDirectory
    var cartridgeDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    protected fun createStructure(target: File, filter: List<String>, productionCartridges: Set<String>) {
        val cfg = project.configurations.getByName(CONFIGURATION_EXTERNALCARTRIDGES)
        val libsCS = project.copySpec()

        cfg.allDependencies.forEach { dependency ->
            if( dependency is ExternalModuleDependency) {
                if(productionCartridges.isEmpty() || productionCartridges.contains(dependency.name)) {
                    project.logger.info("Process external cartridge '{}'.", dependency.name)
                    val staticFile = getStaticFileFor(dependency)
                    project.logger.info("{}: Process static file {}.", dependency.name, staticFile)
                    fsOps.run {
                        sync {
                            it.from(project.zipTree(staticFile))
                            it.into(File(target, "${dependency.name}/release"))
                        }
                    }

                    val jarFile = getJarFileFor(dependency)
                    project.logger.info("{}: Process jar file {}.", dependency.name, jarFile)
                    fsOps.run {
                        sync {
                            it.from(jarFile)
                            it.into(File(target, "${dependency.name}/release/lib/"))
                        }
                    }

                    val libFiles = getLibsFor(dependency, filter)
                    libFiles.forEach { lib ->
                        project.logger.info("{}: Copy {} to {}.", dependency.name, lib.key, lib.value)
                        libsCS.from(lib.key).rename(lib.key.name, lib.value)
                    }
                }
            }
        }
        fsOps.run {
            sync {
                it.with(libsCS)
                it.into(File(target, "libs"))
            }
        }
    }

    private fun getStaticFileFor(dependency: ExternalModuleDependency): File {
        val dep = dependency.copy()
        dep.artifact {
            it.name = dep.name
            it.classifier = "staticfiles"
            it.extension = "zip"
            it.type = "zip"
        }
        val dcfg = project.configurations.detachedConfiguration(dep)
        dcfg.isTransitive = false
        val files = dcfg.resolve()

        return files.first()
    }

    private fun getJarFileFor(dependency: ExternalModuleDependency): File {
        val dep = dependency.copy()
        val dcfg = project.configurations.detachedConfiguration(dep)
        dcfg.isTransitive = false
        val files = dcfg.resolve()

        return files.first()
    }

    private fun getLibsFor(dependency: ExternalModuleDependency, filter: List<String>): Map<File, String> {
        val files  = mutableMapOf<File, String>()

        val dep = dependency.copy()
        val dcfg = project.configurations.detachedConfiguration(dep)
        dcfg.isTransitive = true

        dcfg.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is DefaultModuleComponentArtifactIdentifier) {
                val identifier = artifact.id
                if(identifier is DefaultModuleComponentArtifactIdentifier) {
                    val id = "${identifier.componentIdentifier.group}-" +
                            "${identifier.componentIdentifier.module}-" +
                            identifier.componentIdentifier.version
                    val name = "${id}.${artifact.type}"

                    if(! CartridgeUtil.isCartridge(project, identifier.componentIdentifier) && ! filter.contains(id)) {
                        files[artifact.file] = name
                    }
                } else {
                    throw GradleException("Artifact ID is not a module identifier.")
                }
            }
        }

        return files
    }

    /**
     * Main task function.
     */
    @TaskAction
    open fun processDependencies() {
        // call for incremental build ...
        externalCartridgeDependencies

        val libs = mutableListOf<String>()
        baseProjects.forEach {
            val file = downloadLibFilter(project, it.value.dependency, it.key)
            if(file != null) {
                libs.addAll(file.readLines())
            }
        }
        createStructure(outputDirProperty.get().asFile, libs, productionCartridges)
    }
}
