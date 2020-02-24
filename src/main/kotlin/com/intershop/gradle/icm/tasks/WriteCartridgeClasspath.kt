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

import com.intershop.gradle.icm.cartridge.CartridgePlugin
import com.intershop.gradle.icm.cartridge.CartridgePlugin.Companion.CONFIGURATION_CARTRIDGERUNTIME
import com.intershop.gradle.icm.extension.IntershopExtension.Companion.INTERSHOP_GROUP_NAME
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
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
    private val jarTaskNameProperty: Property<String> = project.objects.property(String::class.java)
    private val useClassesFolderProperty: Property<Boolean> = project.objects.property(Boolean::class.java)

    companion object {
        const val DEFAULT_NAME = "writeCartridgeClasspath"

        const val CARTRIDGE_CLASSPATH_DIR = "classpath"
        const val CARTRIDGE_CLASSPATH_FILE = "cartridge.classpath"
    }

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Writes the classpath of the cartridge to a file."

        outputFileProperty.set(File(project.buildDir, "${CARTRIDGE_CLASSPATH_DIR}/${CARTRIDGE_CLASSPATH_FILE}"))
        jarTaskNameProperty.convention("jar")
        useClassesFolderProperty.convention(false)
    }

    /**
     * Set provider for jar task name property.
     *
     * @param jarTaskName set provider for name of jar task.
     */
    @Suppress( "unused")
    fun provideJarTaskName(jarTaskName: Provider<String>) =
        jarTaskNameProperty.set(jarTaskName)

    @get:Input
    var jarTaskName by jarTaskNameProperty

    /**
     * Set provider for using classes folder instead of jar files.
     *
     * @param useClassesFolder set provider for using classes folder instead of jar files.
     */
    @Suppress( "unused")
    fun provideUseClassesFolder(useClassesFolder: Provider<Boolean>) =
        useClassesFolderProperty.set(useClassesFolder)

    @get:Input
    var useClassesFolder by useClassesFolderProperty

    @get:InputFiles
    val jarFiles: FileCollection by lazy {
        val returnFiles = project.files()

        val jarTask = project.tasks.findByName(jarTaskName)
        if(jarTask != null) {
            returnFiles.setFrom(jarTask.outputs.files.singleFile)
        }

        returnFiles
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

    @get:Input
    val cartridgeRuntimeDependencies: List<String> by lazy {
        val returnDeps = mutableListOf<String>()
        project.configurations.getByName(CartridgePlugin.CONFIGURATION_CARTRIDGERUNTIME).dependencies.forEach {
            returnDeps.add(it.toString())
        }
        returnDeps
    }

    @get:Classpath
    val classpathFiles : FileCollection by lazy {
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

        val runtimeFiles = if (project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            project.configurations.getByName(CONFIGURATION_CARTRIDGERUNTIME).resolvedConfiguration.files
        } else {
            project.files()
        }

        val fileSet = runtimeFiles + classpathFiles.files
        val rootProjectDir = getNormalizedFilePath(project.rootProject.projectDir)
        val buildDirName = project.buildDir.name
        val regex = Regex(".*${buildDirName}/libs/.*")

        outputFile.printWriter().use { out ->
            fileSet.toSortedSet().forEach { cpFile ->
                val path = getNormalizedFilePath(cpFile)
                if(useClassesFolderProperty.get()) {
                    if (path.startsWith(rootProjectDir) && path.matches(regex)) {
                        val projectDirPath =
                            path.replace("${buildDirName}[/|\\\\]libs[/|\\\\].*".toRegex(), "")
                                .replace("\\", "/")
                        out.println("${projectDirPath}/resources/main")
                        out.println("${projectDirPath}/classes/java/main")
                    } else {
                        out.println(path)
                    }
                } else {
                    if(! checkForSource(path)) {
                        out.println(path)
                    }
                }
            }
            if(!useClassesFolderProperty.get()) {
                jarFiles.files.forEach { jarFile ->
                    out.println(getNormalizedFilePath(jarFile))
                }
            }
        }
    }

    private fun checkForSource(filePath: String) : Boolean {
        val buildPath = getNormalizedFilePath(project.buildDir)
        if(filePath.startsWith("${buildPath}/classes") || filePath.startsWith("${buildPath}/resources")) {
            return true
        }
        return false
    }

    private fun getNormalizedFilePath(file: File) : String {
        return file.absolutePath.replace("\\", "/")
    }
}
