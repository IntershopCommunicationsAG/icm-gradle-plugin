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

import com.intershop.gradle.icm.utils.DependencyListUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject

/**
 * Collects all libraries (recursively through all (sub-)projects) and write IDs to a file
 */
open class CreateLibList @Inject constructor(
    objectFactory: ObjectFactory ) : DefaultTask() {

    private val copiedLibrariesDirectoryProperty: Property<Directory> =
        project.objects.directoryProperty().convention(project.layout.buildDirectory.dir(BUILD_FOLDER))

    companion object {
        const val DEFAULT_NAME = "CreateLibraries"
        const val BUILD_FOLDER = "libraries"

        fun getName(type: String): String {
            return "${type.lowercase()}${DEFAULT_NAME}"
        }
        fun getOutputPath(type: String): String {
            return "librarylist/${type.lowercase()}/file.list"
        }
    }

    @get:InputFiles
    val exludeLibraryLists: ListProperty<RegularFile> = objectFactory.listProperty(RegularFile::class.java)

    @get:Input
    val environmentType: Property<String> = objectFactory.property(String::class.java)

    @get:InputFiles
    val cartridgeDescriptors: ListProperty<RegularFile> = objectFactory.listProperty(RegularFile::class.java)

    @get:OutputFile
    val libraryListFile: RegularFileProperty = objectFactory.fileProperty()

    init {
        group = "ICM server build"
        description = "Create a list file of dependencies (recursively through all (sub-)projects) for an environment"
    }

    /**
     * Task action starts the java process in the background.
     */
    @TaskAction
    fun execute() {
        val dependencies = mutableSetOf<String>()
        val dependenciesVersions = mutableMapOf<String, MutableMap<String, String>>()
        cartridgeDescriptors.get().forEach {
            val props = getCartridgeProperties(it)
            dependencies.addAll(getLibraryIDs(props).apply {
                this.forEach {
                    val groupAndName = it.substringBeforeLast(':')
                    val versionInProjects = dependenciesVersions.computeIfAbsent(groupAndName, { mutableMapOf() })
                    versionInProjects.compute(
                        it,
                        { _, p -> if (null == p) getCartridgeName(props) else p + " " + getCartridgeName(props) })
                }
            })
        }

        val conflicts = dependenciesVersions.filter { e -> 1 < e.value.size }.map { it.value }.toList()
        if (conflicts.isNotEmpty()) {
            throw GradleException(
                "Unable to process libraries. Dependencies ${conflicts}" +
                        " are required by cartridge-projects in non-unique versions."
            )
        }

        exludeLibraryLists.get().forEach {
            val excludeList = DependencyListUtil.getIDList(project.logger,  environmentType.get(), it)
            dependencies.removeAll(excludeList.toSet())
        }

        val sortedDeps = dependencies.toList().sorted()
        val listFile = libraryListFile.asFile.get()

        if(listFile.exists()) {
            listFile.delete()
        }

        listFile.printWriter().use { out ->
            sortedDeps.forEach {
                out.println(it)
            }
        }
    }

    private fun getCartridgeProperties(propsFile: RegularFile): Properties {
        val props = Properties()
        FileInputStream(propsFile.asFile).use {
            props.load(it)
        }
        return props
    }
    private fun getCartridgeName(props:Properties): String {
        return props["cartridge.name"].toString() ?: ""
    }

    private fun getLibraryIDs(props:Properties): Set<String> {
        val dependsOnLibs = props["cartridge.dependsOnLibs"].toString()
        return if (dependsOnLibs.isEmpty()) setOf() else dependsOnLibs.split(";").toSet()
    }
}

