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
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
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
 * This task download a text file with all 3rd party libs in a base projects
 * from the configured dependencies. This files will be excluded from the
 * calculated dependencies.
 *
 * @constructor Creates a task that provides the base libraries filter file.
 */
open class ProvideLibFilter @Inject constructor(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory ) : DefaultTask() {

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
     * This methods provides the dependency of single libfilter file
     * as a dependency to the task. Only a module dependency is allowed.
     * Type and extension is txt. The classifier is 'libs'.
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
        description = "Download a file with a available libs of a distribution."

        outputFile.convention(projectLayout.buildDirectory.file("libfilter/libfilter.txt"))
    }

    /**
     * Task execution method of this task.
     * It downloads the property file and stores the result as output file.
     */
    @TaskAction
    fun downloadFile() {
        val dependency = when {
            fileDependency.isPresent && fileDependency.get().isNotEmpty() -> fileDependency.get()
            baseDependency.isPresent && baseDependency.get().isNotEmpty() -> baseDependency.get()
            else -> null
        }

        if(outputFile.asFile.get().exists()) {
            outputFile.asFile.get().delete()
        }

        if (dependency != null) {
            val resultFile = downloadLibFilter(dependency)
            if (resultFile != null) {
                resultFile.copyTo(outputFile.get().asFile)
            } else {
                outputFile.get().asFile.createNewFile()
            }
        } else {
            outputFile.get().asFile.createNewFile()
        }
    }

    private fun downloadLibFilter(dependency: String): File? {
        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependency) as ExternalModuleDependency

        dep.artifact {
            it.name = dep.name
            it.classifier = "libs"
            it.extension = "txt"
            it.type = "txt"
        }
        val dcfg = project.configurations.detachedConfiguration(dep)

        try {
            val files = dcfg.resolve()
            return files.first()
        } catch (anfe: DefaultLenientConfiguration.ArtifactResolveException) {
            project.logger.warn("No library filter is available!")
        }
        return null
    }
}
