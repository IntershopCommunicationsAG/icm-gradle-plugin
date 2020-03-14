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

import com.intershop.gradle.icm.extension.BaseProjectConfiguration
import com.intershop.gradle.icm.extension.DirConf
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import java.io.File
import javax.inject.Inject

/**
 * Abstract task class to create folders for
 * project or adapter cartridges.
 */
abstract class AbstractCreateFolder @Inject constructor(
    objectFactory: ObjectFactory,
    private var fsOps: FileSystemOperations): DefaultTask() {

    @get:Internal
    val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    private val baseProjectsProperty: MapProperty<String, BaseProjectConfiguration> =
        objectFactory.mapProperty(String::class.java, BaseProjectConfiguration::class.java)

    private val dirConfProperty: Property<DirConf> = objectFactory.property(DirConf::class.java)
    private val devDirConfProperty: Property<DirConf> = objectFactory.property(DirConf::class.java)

    @get:Nested
    var baseProjects: Map<String, BaseProjectConfiguration>
        get() = baseProjectsProperty.get()
        set(value) = baseProjectsProperty.putAll(value)

    /**
     * Provider configuration for project folder.
     *
     * @param dirConf
     */
    fun provideDirConf(dirConf: Provider<DirConf>) = dirConfProperty.set(dirConf)

    /**
     * Configuration for project folder.
     *
     * @property dirConf
     */
    @get:Optional
    @get:Nested
    var dirConf: DirConf?
        get() = if(dirConfProperty.get().dir != null) dirConfProperty.get() else null
        set(value) = dirConfProperty.set(value)

    /**
     * Provider configuration for project developer folder.
     *
     * @param devConf
     */
    fun provideDevDirConf(devDirConf: Provider<DirConf>) = devDirConfProperty.set(devDirConf)

    /**
     * Configuration for project developer configuration folder.
     *
     * @property devConf
     */
    @get:Optional
    @get:Nested
    var devDirConf: DirConf?
        get() = if(devDirConfProperty.isPresent && devDirConfProperty.get().dir != null)
                    devDirConfProperty.get()
                else
                    null
        set(value) = devDirConfProperty.set(value)

    /**
     * Provider configuration for target directory.
     *
     * @param outputDir
     */
    fun provideOutputDir(outputDir: Provider<Directory>) = outputDirProperty.set(outputDir)

    /**
     * Output directory for generated files.
     *
     * @property outputDir
     */
    @get:OutputDirectory
    var outputDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    @get:Internal
    abstract val classifier: String

    protected fun startFolderCreation() {
        val cs = project.copySpec()

        if(dirConf != null) {
            val dirCS = project.copySpec()
            dirCS.from(dirConf?.dir)

            dirConf?.excludes?.forEach {
                dirCS.exclude(it)
            }
            dirConf?.includes?.forEach {
                dirCS.include(it)
            }
            if(! dirConf?.targetPath.isNullOrEmpty()) {
                dirCS.into(dirConf?.targetPath!!)
            }
            if(dirConf != null && dirConf?.duplicateStrategy != DuplicatesStrategy.INHERIT) {
                dirCS.duplicatesStrategy = dirConf!!.duplicateStrategy
            }
        }

        if(devDirConf != null) {
            val devDirCS = project.copySpec()
            devDirCS.from(devDirConf?.dir)

            devDirConf?.excludes?.forEach {
                devDirCS.exclude(it)
            }
            devDirConf?.includes?.forEach {
                devDirCS.include(it)
            }
            if(! devDirConf?.targetPath.isNullOrEmpty()) {
                devDirCS.into(devDirConf?.targetPath!!)
            }
            if(dirConf != null && devDirConf?.duplicateStrategy != DuplicatesStrategy.INHERIT) {
                devDirCS.duplicatesStrategy = devDirConf!!.duplicateStrategy
            }
        }

        baseProjects.forEach {
            var file = downloadPackage(it.value.dependency, classifier)
            var pkgCS = project.copySpec()

            pkgCS.from(project.zipTree(file))
            addCopyConfSpec(cs, pkgCS, it.value, file)
            cs.with(pkgCS)
        }

        fsOps.copy {
            it.with(cs)
            it.into(outputDir)
        }
    }

    /**
     * Add copy spec for an package file.
     *
     * @param cs        base copy spec
     * @param pkgCS     package copy spec
     * @param prjConf   configuration of a base project
     * @param file      package file it self
     */
    abstract fun addCopyConfSpec(cs: CopySpec, pkgCS: CopySpec, prjConf: BaseProjectConfiguration, file: File)

    private fun downloadPackage(dependency: String, classifier: String): File {
        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependency) as ExternalModuleDependency
        dep.artifact {
            it.name = dep.name
            it.classifier = classifier
            it.extension = "zip"
            it.type = "zip"
        }

        val configuration = project.configurations.maybeCreate(configurationName)
        configuration.setVisible(false)
            .setTransitive(false)
            .setDescription("$classifier for package download: ${this.name}")
            .defaultDependencies { ds ->
                ds.add(dep)
            }

        val files = configuration.resolve()
        return files.first()
    }

    private val configurationName: String
        get() = "${this.name.toLowerCase()}_configuration"
}
