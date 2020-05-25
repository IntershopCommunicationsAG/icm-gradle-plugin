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

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
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
        val objectFactory: ObjectFactory,
        private var fsOps: FileSystemOperations) : DefaultTask() {

    @get:Internal
    val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    @get:Input
    val cartridges: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Provides a list of cartridges - module dependencies and project names - to
     * the task. Dependent on the cartridge style and the configured environments
     * the tasks will be downloaded to the project.
     *
     * @param list  provider of a set of strings.
     */
    fun provideCartridges(list: Provider<Set<String>>) = cartridges.set(list)

    @get:Input
    val dbprepareCartridges: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Provides a list of db initialization cartridges - module dependencies and project names -
     * to the task. Dependent on the cartridge style and the configured environments
     * the tasks will be downloaded to the project.
     *
     * @param list  provider of a set of strings.
     */
    fun provideDBprepareCartridges(list: Provider<Set<String>>) = dbprepareCartridges.set(list)

    @get:Optional
    @get:InputFile
    val libFilterFile: RegularFileProperty = objectFactory.fileProperty()

    @get:Optional
    @get:Input
    val projectDependencies: SetProperty<String> = objectFactory.setProperty(String::class.java)

    fun projectDependency(dependency: String) {
        if(dependency.isNotEmpty()) {
            projectDependencies.add(dependency)
        }
    }

    /**
     * Provides a file with a list of installed 3rd party libs in the base project container.
     * See also task ProvideLibFilter.
     *
     * @param libFilter regular file provider.
     */
    fun provideLibFilterFile(libFilter: Provider<RegularFile>) = libFilterFile.set(libFilter)

    @get:Optional
    @get:Input
    val environmentTypes: ListProperty<EnvironmentType> = objectFactory.listProperty(EnvironmentType::class.java)

    /**
     * Provider configuration for target directory.
     *
     * @param cartridgeDir
     */
    fun provideOutputDir(cartridgeDir: Provider<Directory>) = outputDirProperty.set(cartridgeDir)

    /**
     * Output directory of this task.
     *
     * @property cartridgeDir
     */
    @get:OutputDirectory
    var cartridgeDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Create a directory with external cartridges."

        outputDirProperty.convention(projectLayout.buildDirectory.dir("server/cartridges"))

        environmentTypes.convention(listOf(EnvironmentType.PRODUCTION))
    }

    private fun configureUsage(conf: Configuration) {
        conf.attributes {
                it.attribute(Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME))
                it.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objectFactory.named(LibraryElements::class.java, LibraryElements.JAR)
                )
                it.attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category::class.java, Category.LIBRARY))
            }
    }

    private fun createStructure(cartridges: List<String>,
                                  target: File,
                                  filter: List<String>,
                                  environmentTypes: List<EnvironmentType>) {

        val deps = mutableListOf<Dependency>()
        cartridges.forEach { cartridge ->
            deps.add(project.dependencies.create(cartridge))
        }

        val dcfg = project.configurations.detachedConfiguration(*deps.toTypedArray())
        dcfg.isTransitive = false
        configureUsage(dcfg)

        val libsCS = project.copySpec()
        val libFiles = mutableMapOf<File, String>()

        dcfg.allDependencies.forEach { dependency ->

            if( dependency is ExternalModuleDependency &&
                CartridgeUtil.isCartridge(project, dependency, environmentTypes) ) {

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

                libFiles.putAll(getLibsFor(dependency, filter))
            }
        }

        libFiles.forEach { lib ->
            project.logger.info("Add to copyspec {} to {}.", lib.key, lib.value)
            libsCS.from(lib.key) {
                it.rename(".*", lib.value)
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

    @Throws(GradleException::class)
    private fun getLibsFor(dependency: ExternalModuleDependency, filter: List<String>): Map<File, String> {
        val files  = mutableMapOf<File, String>()

        val listDeps = mutableListOf<Dependency>()
        val dep = dependency.copy()

        projectDependencies.get().forEach {
            listDeps.add(project.dependencies.enforcedPlatform(it))
        }

        listDeps.add(dep)

        val dcfg = project.configurations.detachedConfiguration(*listDeps.toTypedArray())
        dcfg.isTransitive = true

        dcfg.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is DefaultModuleComponentArtifactIdentifier) {
                val identifier = artifact.id
                if(identifier is DefaultModuleComponentArtifactIdentifier) {
                    val id = CartridgeUtil.getFileIDFrom(identifier.componentIdentifier)

                    logger.info("Check artifact id '{}'", id)
                    val name = "${id}.${artifact.type}"

                    if(! CartridgeUtil.isCartridge(project, identifier.componentIdentifier) && ! filter.contains(id)) {
                        logger.info("Add artifact {} with name '{}'", artifact.file, name)
                        files[artifact.file] = name
                    } else {
                        logger.info("Add artifact {} with name '{}' was not added.", artifact.file, name)
                    }
                } else {
                    throw GradleException("Artifact ID is not a module identifier.")
                }
            } else {
                logger.info("Artifact id {} is not a DefaultModuleComponentArtifactIdentifier", artifact.id)
            }
        }

        return files
    }

    /**
     * Main task function.
     */
    @TaskAction
    open fun processDependencies() {

        val extCartridges = mutableListOf<String>()

        cartridges.get().forEach { cartridge ->
            if(CartridgeUtil.isModuleDependency(cartridge)) {
                extCartridges.add(cartridge)
            }
        }

        dbprepareCartridges.get().forEach { cartridge ->
            if(CartridgeUtil.isModuleDependency(cartridge)) {
                extCartridges.add(cartridge)
            }
        }

        val libFilter = mutableListOf<String>()
        val filterFile = libFilterFile.orNull
        if(filterFile != null) {
            libFilter.addAll(filterFile.asFile.readLines())
        }

        createStructure(extCartridges, cartridgeDir, libFilter, environmentTypes.get())
    }
}
