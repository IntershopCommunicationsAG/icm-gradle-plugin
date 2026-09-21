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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Collects all libraries (recursively through all (sub-)projects)
 */
@CacheableTask
abstract class CopyLibraries @Inject constructor(
        objectFactory: ObjectFactory,
        private val fsOps: FileSystemOperations ) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "CopyLibraries"

        fun getName(type: String): String {
            return "${type.lowercase()}${DEFAULT_NAME}"
        }
        fun getOutputPath(type: String): String {
            return "libraries/${type.lowercase()}"
        }
    }

    init {
        group = "ICM server build"
        description = "Copy libraries from a list of dependencies for an environment"
    }

    @get:Input
    val environmentType: Property<String> = objectFactory.property(String::class.java)

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val dependencyIDFile: RegularFileProperty = objectFactory.fileProperty()

    @get:OutputDirectory
    val librariesDirectory: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * The libraries to be copied, mapped from their target file name
     * '''${dependency.moduleGroup}_${dependency.moduleName}_${dependency.moduleVersion}.${artifact.extension}'''
     * to the resolved artifact file.
     *
     * The dependencies are resolved by the plugin at configuration time, so that the task neither accesses
     * the project nor resolves a configuration during execution. The libraries are derived from the content
     * of {@link #dependencyIDFile}, which is tracked as an input of this task.
     *
     * This property must not be an input: the dependency IDs are written by the task that produces
     * {@link #dependencyIDFile}, so the configuration can only be resolved after that task has run.
     * Declaring it as an input would force the resolution while the task graph is built, which fails
     * because the list file does not exist yet.
     *
     * @property resolvedLibraries
     */
    @get:Internal
    abstract val resolvedLibraries: MapProperty<String, File>

    /**
     * Task action copies the resolved libraries to the libraries directory.
     */
    @TaskAction
    fun execute() {
        val libraries = resolvedLibraries.get()

        fsOps.sync { spec ->
            spec.into(librariesDirectory)

            libraries.forEach { (targetName, libraryFile) ->
                spec.from(libraryFile) { fileSpec ->
                    fileSpec.rename { targetName }
                }
            }
        }
    }
}

