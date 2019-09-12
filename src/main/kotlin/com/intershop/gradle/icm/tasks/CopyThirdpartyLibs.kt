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

import com.intershop.gradle.icm.ICMBuildPlugin.Companion.THIRDPARTYLIB_DIR
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import java.io.File

/**
 * CopyThirdpartyLibs Gradle task 'copyThirdpartyLibs'
 *
 * This task copy all dependend thirdparty dependencies to
 * a lib folder. This is used for the build of containerimages.
 */
open class CopyThirdpartyLibs : DefaultTask() {

    private val outputDirProperty: DirectoryProperty = project.objects.directoryProperty()

    init {
        outputDirProperty.set(File(project.buildDir, THIRDPARTYLIB_DIR ))
    }

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
    private val configurationClasspath: FileCollection by lazy {
        val returnFiles = project.files()

        if (project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            returnFiles.from(project.configurations.getByName("runtimeClasspath").files)
        }

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
        if(outputDir.listFiles().size > 0) {
            outputDir.deleteRecursively()
            outputDir.mkdirs()
        }

        project.configurations.getByName("runtimeClasspath")
            .resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is DefaultModuleComponentArtifactIdentifier) {

                var identifier = artifact.id
                if(identifier is DefaultModuleComponentArtifactIdentifier) {
                    var name = "${identifier.componentIdentifier.group}-" +
                            "${identifier.componentIdentifier.module}-" +
                            "${identifier.componentIdentifier.version}.${artifact.type}"

                    artifact.file.copyTo(
                        File(outputDir, name),
                        overwrite = true
                    )
                } else {
                    throw GradleException("Artifact ID is not a module identifier.")
                }
            }
        }
    }
}
