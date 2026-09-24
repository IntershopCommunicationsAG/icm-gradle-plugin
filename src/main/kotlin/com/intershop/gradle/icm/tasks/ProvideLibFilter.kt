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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * This task download a text file with all 3rd party libs in a base projects
 * from the configured dependencies. This files will be excluded from the
 * calculated dependencies.
 *
 * @constructor Creates a task that provides the base libraries filter file.
 */
@CacheableTask
abstract class ProvideLibFilter @Inject constructor(
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
     * The resolved lib filter file of the configured dependency.
     *
     * The dependency is resolved by the plugin at configuration time through a lenient artifact view, so
     * that the task neither accesses the project nor resolves a configuration during execution. The
     * collection is empty if no dependency is configured or if it provides no lib filter artifact.
     *
     * @property libFilterFiles
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    val libFilterFiles: ConfigurableFileCollection = objectFactory.fileCollection()

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
        val target = outputFile.asFile.get()
        if(target.exists()) {
            target.delete()
        }

        val resultFile = libFilterFiles.files.firstOrNull()

        if (resultFile != null) {
            resultFile.copyTo(target)
        } else {
            if (isDependencyConfigured()) {
                logger.warn("No library filter is available!")
            }
            target.createNewFile()
        }
    }

    private fun isDependencyConfigured(): Boolean =
            fileDependency.getOrElse("").isNotEmpty() || baseDependency.getOrElse("").isNotEmpty()
}
