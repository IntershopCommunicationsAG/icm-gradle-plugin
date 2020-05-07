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

import com.intershop.gradle.icm.extension.IntershopExtension.Companion.INTERSHOP_GROUP_NAME
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import javax.inject.Inject

/**
 * CopyThirdpartyLibs Gradle task 'copyThirdpartyLibs'
 *
 * This task copy all dependend thirdparty dependencies to
 * a lib folder. This is used for the build of containerimages.
 */
open class CopyThirdpartyLibs @Inject constructor(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "copyThirdpartyLibs"
        const val THIRDPARTYLIB_DIR = "lib"
    }

    /**
     * Provider configuration for target directory.
     *
     * @param dir
     */
    fun provideOutputDir(dir: Provider<Directory>) = outputDir.set(dir)

    /**
     * Output directory for generated files.
     *
     * @property outputDir
     */
    @get:OutputDirectory
    val outputDir: DirectoryProperty = objectFactory.directoryProperty()

    @get:Optional
    @get:InputFile
    val libFilterFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Provides a file with a special list of used libraries in the base
     * project. The entry in the list is <group name>-<module name>-<version>.
     *
     * @param file regular file provider.
     */
    fun provideLibFilterFile(file: Provider<RegularFile>) = libFilterFile.set(file)

    @get:Classpath
    val configurationClasspath: FileCollection by lazy {
        val returnFiles = project.files()
        returnFiles.from(project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME).files)
        returnFiles
    }

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Copy all thirdparty libs to a directory."

        outputDir.convention(projectLayout.buildDirectory.dir(THIRDPARTYLIB_DIR))
    }

    /**
     * Task method for the creation of a descriptor file.
     */
    @Suppress("unused")
    @Throws(GradleException::class)
    @TaskAction
    fun runCopy() {
        // we are not sure what is changed.
        if(outputDir.isPresent && outputDir.get().asFileTree.files.isNotEmpty()) {
            outputDir.get().asFile.deleteRecursively()
            outputDir.get().asFile.mkdirs()
        }

        val libs = mutableListOf<String>()

        if(libFilterFile.isPresent && libFilterFile.get().asFile.exists()) {
            libs.addAll(libFilterFile.get().asFile.readLines())
        }

        if(libs.isEmpty()) {
            project.logger.debug("No lib filter entries available.")
        }

        //call for incremental task execution
        configurationClasspath

        project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            .resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is DefaultModuleComponentArtifactIdentifier) {

                val identifier = artifact.id
                if(identifier is DefaultModuleComponentArtifactIdentifier) {
                    val id = "${identifier.componentIdentifier.group}-" +
                             "${identifier.componentIdentifier.module}-" +
                             identifier.componentIdentifier.version
                    val name = "${id}.${artifact.type}"

                    if(! libs.contains(id)) {
                        artifact.file.copyTo(
                            outputDir.file(name).get().asFile,
                            overwrite = true
                        )
                    }
                } else {
                    throw GradleException("Artifact ID is not a module identifier.")
                }
            }
        }
    }
}
