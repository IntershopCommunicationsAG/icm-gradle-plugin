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
package com.intershop.gradle.icm.tasks.crossproject

import com.intershop.gradle.icm.CrossProjectDevelopmentPlugin.Companion.CROSSPRJ_CONFPATH
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class WriteMappingFile
    @Inject constructor(projectLayout: ProjectLayout,
                        objectFactory: ObjectFactory): DefaultTask() {

    /**
     * Output file for generated cluster id.
     *
     * @property outputFile
     */
    @get:OutputDirectory
    val outputDir: DirectoryProperty = objectFactory.directoryProperty()

    init {
        group = "ICM Cross-Project Development"
        description = "Creates mappings files, like settings.gradle.kts include files."

        outputDir.convention(projectLayout.projectDirectory.dir("${CROSSPRJ_CONFPATH}/${project.name}"))
    }

    @TaskAction
    fun writeFile() {
        val file = outputDir.get().file("mapping.gradle.kts").asFile
        val prjFile = outputDir.get().file("projectmapping.conf").asFile

        if(! file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        recreateFile(file)
        recreateFile(prjFile)

        file.appendText(
            """
            includeBuild("${project.projectDir.absolutePath}") {
                dependencySubstitution {
            """.trimIndent(), Charsets.UTF_8)

        file.appendText("\n", Charsets.UTF_8)

        project.rootProject.subprojects {
            file.appendText(
                "        substitute(module(\"${it.group}:${it.name}\"))"+
                                            ".with(project(\"${normalizePath(it.path)}\"))\n")
        }

        file.appendText(
            """
                }
            }
            """.trimIndent(), Charsets.UTF_8)

        prjFile.appendText("${project.name} = ${project.group}_${project.name}")
    }

    private fun recreateFile(file: File) {
        if(file.exists()) {
            file.delete()
        } else {
            file.createNewFile()
        }
    }

    private fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }
}
