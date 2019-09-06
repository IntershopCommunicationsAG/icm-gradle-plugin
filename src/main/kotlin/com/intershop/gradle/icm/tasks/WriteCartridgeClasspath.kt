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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * WriteCartridgeClasspath Gradle task 'writeCartridgeClasspath'
 *
 * This task writes all classpath entries of runtime classpath
 * to an file. This file is used by some tools and tests.
 */
open class WriteCartridgeClasspath : DefaultTask() {

    private val outputFileProperty: RegularFileProperty = project.objects.fileProperty()
    private val addJarFileProperty: RegularFileProperty = project.objects.fileProperty()

    init {
        outputFileProperty.set(File(project.buildDir,
            "${ICMBuildPlugin.CARTRIDGE_CLASSPATH_DIR}/${ICMBuildPlugin.CARTRIDGE_CLASSPATH_FILE}"))

        var jarTask = project.tasks.findByName("jar")
        if(jarTask != null) {
            addJarFileProperty.set(jarTask.outputs.files.first())
            this.dependsOn(jarTask)
        }
    }

    /**
     * The output file contains the classpath entries of the cartridge.
     *
     * @property outputFile real file on file system with descriptor
     */
    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)

    /**
     * The input of a single jar file if available.
     *
     * @property addJarFile additional jar file for classpath creation. This should be the jar file of the jar task.
     */
    @get:InputFile
    var addJarFile: File
        get() = addJarFileProperty.get().asFile
        set(value) = addJarFileProperty.set(value)

    @get:Classpath
    private val cartridgeRuntimelist: FileCollection by lazy {
        val returnFiles = project.files()

        if (project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            returnFiles.from(project.configurations.getByName("cartridgeRuntime").files)
        }

        returnFiles
    }

    @get:Classpath
    private val classpathfiles : FileCollection by lazy {
        val returnFiles = project.files()

        // search all files for classpath
        if(project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
            val mainSourceSet = javaConvention.sourceSets.getByName("main")

            returnFiles.from(mainSourceSet.runtimeClasspath)
        }

        returnFiles
    }

    /**
     * Task method for the creation of a descriptor file.
     */
    @Suppress("unused")
    @TaskAction
    fun runFileCreation() {
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        if (outputFile.exists()) {
            outputFile.delete()
        }

        var fileSet = cartridgeRuntimelist.files + classpathfiles.files

        outputFile.printWriter().use { out ->
            out.println(getNormalizedFilePath(addJarFile))
            fileSet.toSortedSet().forEach { cpFile ->
                val path = getNormalizedFilePath(cpFile)
                if(! checkForSource(path)) {
                    out.println(cpFile.absolutePath)
                }
            }
        }
    }

    private fun getNormalizedFilePath(file: File) : String {
        return file.absolutePath.replace("\\", "/")
    }

    private fun checkForSource(filePath: String) : Boolean {
        var buildPath = getNormalizedFilePath(project.buildDir)
        if(filePath.startsWith("${buildPath}/classes") || filePath.startsWith("${buildPath}/resources")) {
            return true
        }
        return false
    }
}
