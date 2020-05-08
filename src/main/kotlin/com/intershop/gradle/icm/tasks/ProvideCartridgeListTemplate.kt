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
import com.intershop.gradle.icm.tasks.ExtendCartridgeList.Companion.CARTRIDGELISTFILE_NAME
import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * This task download and extract, if necessary, the cartridgelist.properties
 * from the configured dependencies.
 *
 * @property fsOps service object for file system operations.
 * @constructor Creates a task that provides the base cartridgelist.properties.
 */
open class ProvideCartridgeListTemplate
    @Inject constructor(
        projectLayout: ProjectLayout,
        objectFactory: ObjectFactory,
        private var fsOps: FileSystemOperations) : DefaultTask() {

    @get:Optional
    @get:Input
    val baseDependency: Property<String> = objectFactory.property(String::class.java)

    /**
     * This methods provides the dependency of the base project to the task.
     * Only a module dependency is allowed.
     *
     * @param dependency   dependency of the base project.
     */
    fun provideBaseDependency(dependency: Provider<String>) = baseDependency.set(dependency)

    @get:Optional
    @get:Input
    val fileDependency: Property<String> = objectFactory.property(String::class.java)

    /**
     * This methods provides the dependency of single cartridgelist.properties
     * as a dependency to the task. Only a module dependency is allowed.
     * Type and extension is properties. The classifier is 'cartridgelist'.
     *
     * @param dependency   dependency of the file.
     */
    fun provideFileDependency(dependency: Provider<String>) = fileDependency.set(dependency)

    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Provide the output file for the task.
     *
     * @param file regular file provider.
     */
    fun provideOutputFile(file: Provider<RegularFile>) = outputFile.set(file)

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Download a file cartridgelist.properties for further steps."

        outputFile.convention(projectLayout.buildDirectory.file("cartridgelisttemplate/cartridgelist.properties"))
    }

    /**
     * Task execution method of this task.
     * It downloads the property file and stores the result as output file.
     */
    @TaskAction
    fun downloadFile() {

        if(outputFile.asFile.get().exists()) {
            outputFile.asFile.get().delete()
        }

        if(fileDependency.isPresent && fileDependency.get().isNotEmpty()) {
            val file = downloadPropertiesFile(fileDependency.get())
            if(file != null) {
                copyFile(file)
            } else {
                throw GradleException("Configured file dependency is not available (${fileDependency.get()}).")
            }
        } else {
            val file = PackageUtil.downloadPackage(project, baseDependency.get(), "configuration")
            if(file != null) {
                val pfiles = project.zipTree(file).matching { pf ->
                    pf.include("**/**/${CARTRIDGELISTFILE_NAME}")
                }
                if (!pfiles.isEmpty) {
                    copyFile(pfiles.files.first())
                }
            } else {
                throw GradleException("Configuration package is not available " +
                        "in the configured base project (${baseDependency.get()})")
            }
        }
    }

    private fun copyFile(source: File) {
        fsOps.copy {
            it.from(source).rename(".*", outputFile.get().asFile.name)
            it.into(outputFile.get().asFile.parent)
        }
    }

    private fun downloadPropertiesFile(dependency: String) : File? {
        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependency) as ExternalModuleDependency

        dep.artifact {
            it.name = dep.name
            it.classifier = "cartridgelist"
            it.extension = "properties"
            it.type = "properties"
        }
        val dcfg = project.configurations.detachedConfiguration(dep)

        try {
            val files = dcfg.resolve()
            return files.first()
        } catch (anfe: DefaultLenientConfiguration.ArtifactResolveException) {
            project.logger.warn("No cartridge list is available!")
        }
        return null
    }
}
