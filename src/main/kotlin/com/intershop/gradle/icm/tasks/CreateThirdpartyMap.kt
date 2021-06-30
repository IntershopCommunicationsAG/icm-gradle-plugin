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
import com.intershop.gradle.icm.utils.CartridgeUtil
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
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier
import java.io.File
import javax.inject.Inject

/**
 * CopyThirdpartyLibs Gradle task 'createThirdpartyMap'
 *
 * This task creates a map of all dependend thirdparty dependencies to
 * a text file. This is used for the build of containerimages.
 */
open class CreateThirdpartyMap @Inject constructor(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "createThirdpartyMap"
        const val THIRDPARTYMAP_DIR = "libmap"
        const val FILENAME = "thirdpartlib.map"
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
    @get:IgnoreEmptyDirectories
    val configurationClasspath: FileCollection by lazy {
        val returnFiles = project.files()
        returnFiles.from(project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME).files)
        returnFiles
    }

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Creates a map with thirdparty libs in a directory."

        outputDir.convention(projectLayout.buildDirectory.dir(THIRDPARTYMAP_DIR))
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

        val resultMap = mutableMapOf<String, File>()

        project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            .resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is ModuleComponentArtifactIdentifier) {
                val identifier : ModuleComponentArtifactIdentifier = artifact.id as ModuleComponentArtifactIdentifier
                val id = "${identifier.componentIdentifier.group}-" +
                         "${identifier.componentIdentifier.module}-" +
                         identifier.componentIdentifier.version
                val name = "${id}.${artifact.type}"

                if(! CartridgeUtil.isCartridge(project, identifier.componentIdentifier) && ! libs.contains(id)) {
                    resultMap[name] = artifact.file
                }
            }
        }

        val outputFile = outputDir.file(FILENAME).get().asFile

        outputFile.bufferedWriter().use { out ->
            resultMap.forEach { name, _ ->
                out.write("$name = ${getNormalizedFilePath(File(path))}")
                out.newLine()
            }
        }
    }

    private fun getNormalizedFilePath(file: File) : String {
        return file.absolutePath.replace("\\", "/")
    }
}
