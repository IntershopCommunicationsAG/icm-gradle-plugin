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
import com.intershop.gradle.icm.extension.IntershopExtension.Companion.INTERSHOP_GROUP_NAME
import com.intershop.gradle.icm.tasks.CartridgeUtil.downloadLibFilter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import java.io.File
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

    private val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()
    private val baseProjectsProperty: MapProperty<String, BaseProjectConfiguration> =
        objectFactory.mapProperty(String::class.java, BaseProjectConfiguration::class.java)

    companion object {
        const val DEFAULT_NAME = "copyThirdpartyLibs"
        const val THIRDPARTYLIB_DIR = "lib"
    }

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Copy all thirdparty libs to a directory."

        outputDirProperty.convention(projectLayout.buildDirectory.dir(THIRDPARTYLIB_DIR))
    }

    @get:Nested
    var baseProjects: Map<String, BaseProjectConfiguration>
        get() = baseProjectsProperty.get()
        set(value) = baseProjectsProperty.putAll(value)

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

    @get:Classpath
    val configurationClasspath: FileCollection by lazy {
        val returnFiles = project.files()
        returnFiles.from(project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME).files)
        returnFiles
    }

    /**
     * Task method for the creation of a descriptor file.
     */
    @Suppress("unused")
    @Throws(GradleException::class)
    @TaskAction
    fun runCopy() {
        // we are not sure what is changed.
        if(outputDir.listFiles().isNotEmpty()) {
            outputDir.deleteRecursively()
            outputDir.mkdirs()
        }

        val libs = mutableListOf<String>()
        baseProjects.forEach {
            val file = downloadLibFilter(project, it.value.dependency, it.key)
            if(file != null) {
                libs.addAll(file.readLines())
            }
        }
        if(libs.isEmpty()) {
            project.logger.info("No lib filter entries available.")
        }

        project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            .resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is DefaultModuleComponentArtifactIdentifier) {

                val identifier = artifact.id
                if(identifier is DefaultModuleComponentArtifactIdentifier) {
                    val id = "${identifier.componentIdentifier.group}-" +
                             "${identifier.componentIdentifier.module}-" +
                             identifier.componentIdentifier.version
                    val name = "${id}.${artifact.type}"

                    if(libs.isEmpty() || ! libs.contains(id)) {
                        artifact.file.copyTo(
                            File(outputDir, name),
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
