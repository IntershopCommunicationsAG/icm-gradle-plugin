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

import com.intershop.gradle.icm.ICMBuildPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class WriteCartridgeClasspath : DefaultTask() {

    private val outputFileProperty: RegularFileProperty = project.objects.fileProperty()

    init {
        outputFileProperty.set(File(project.buildDir, "${ICMBuildPlugin.CARTRIDGE_CLASSPATH_DIR}/${ICMBuildPlugin.CARTRIDGE_CLASSPATH_FILE}"))
    }

    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)

    @get:Classpath
    private val classpath: FileCollection by lazy {
        val returnFiles = project.files()

        if (project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
            val mainSourceSet = javaConvention.sourceSets.getByName("main")

            returnFiles.from(mainSourceSet.output.classesDirs, mainSourceSet.runtimeClasspath)
        }

        returnFiles
    }

    @TaskAction
    fun runFileCreation() {
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        if (outputFile.exists()) {
            outputFile.delete()
        }

        outputFile.printWriter().use { out ->
            classpath.forEach { cpFile ->
                out.println(cpFile.absolutePath)
            }
        }
    }
}