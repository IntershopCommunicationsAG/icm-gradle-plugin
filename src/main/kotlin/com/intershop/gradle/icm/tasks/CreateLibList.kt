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
import org.gradle.util.internal.VersionNumber
import java.io.FileInputStream
import java.util.Properties
import javax.inject.Inject

/**
 * Collects all libraries (recursively through all (sub-)projects) and write IDs to a file
 */
open class CreateLibList @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {

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
        // map group+name to another map mapping group+name+version to cartridgeNames (space separated list)
        // e.g. "aopalliance:aopalliance" -> {
        //      "1.0" -> "ac_order_export_xml_b2b ac_cxml_order_injection",
        //      "1.0.1" -> "ac_order_export_xml_b2b ac_cxml_order_injection"
        // }
        val dependenciesVersions = mutableMapOf<String, MutableMap<String, String>>()
        cartridgeDescriptors.get().forEach {
            val props = getCartridgeProperties(it)
            getLibraryIDs(props).forEach { libraryID ->
                val groupAndName = libraryID.substringBeforeLast(':')
                val version = libraryID.substringAfterLast(':')
                val versionToCartridges = dependenciesVersions.computeIfAbsent(groupAndName) { mutableMapOf() }
                versionToCartridges.compute(version) { _, cartNames ->
                    if (null == cartNames) getCartridgeName(props) else cartNames + " " + getCartridgeName(props)
                }
            }
        }

        val dependencies = dependenciesVersions.toSortedMap().map { (groupAndName, versionToCartridges) ->
            toDependencyId(groupAndName, resolveVersionConflict(groupAndName, versionToCartridges))
        }.toMutableList()

        exludeLibraryLists.get().forEach {
            val excludeList = DependencyListUtil.getIDList(environmentType.get(), it)
            dependencies.removeAll(excludeList.toSet())
        }

        val sortedDeps = dependencies.toList().sorted()
        val listFile = libraryListFile.asFile.get()

        if (listFile.exists()) {
            listFile.delete()
        }

        listFile.printWriter().use { out ->
            sortedDeps.forEach {
                out.println(it)
            }
        }
    }

    private fun resolveVersionConflict(groupAndName: String, versionToCartridges: Map<String, String>): String {
        // not a conflict at all ?
        if (versionToCartridges.size == 1) {
            return versionToCartridges.keys.first()
        }

        project.logger.debug("Trying to resolve a version conflict for dependency '{}' requiring the versions {}",
                groupAndName, versionToCartridges.keys)

        // parse to org.gradle.util.internal.VersionNumber
        val versions = versionToCartridges.keys.map { versionStr ->
            val versionNumber = VersionNumber.parse(versionStr)
            if (versionNumber == VersionNumber.UNKNOWN) {
                throw GradleException(
                        "The version string '$versionStr' can not be parsed into a ${VersionNumber::class.java} " +
                        "therefore the conflict resolution must be done manually: The dependency '$groupAndName' is " +
                        "required in ${versionToCartridges.size} versions by the following cartridges: " +
                        "$versionToCartridges")
            }
            versionNumber
        }.sortedDescending()

        // check for major/minor/patch version jump
        var prev: VersionNumber? = null
        for (curr in versions) {
            if (prev == null) {
                prev = curr
                continue // skip first iteration (nothing to compare)
            }
            if (prev.major != curr.major) {
                throw GradleException(
                        "There's a major version conflict for dependency '$groupAndName': $prev <-> $curr. Please " +
                        "resolve this conflict analyzing the dependencies of the following cartridges: " +
                        "$versionToCartridges")
            }
            val chosen = maxOf(prev, curr)
            if (prev.minor != curr.minor) {
                project.logger.warn(
                        "There's a minor version conflict for dependency '{}': {} <-> {}. Version {} is chosen. If " +
                        "this is not the correct version please resolve this conflict analyzing the dependencies of " +
                        "the following cartridges: {}", groupAndName, prev, curr, chosen, versionToCartridges)
            }
            if (prev.micro != curr.micro) {
                project.logger.info(
                        "There's a patch version conflict for dependency '{}': {} <-> {}. Version {} is chosen. If " +
                        "this is not the correct version please resolve this conflict analyzing the dependencies of " +
                        "the following cartridges: {}", groupAndName, prev, curr, chosen, versionToCartridges)
            }
            prev = curr
        }

        // finally take the first (highest) version
        val chosen = versions.first()
        project.logger.debug("Resolved the version conflict for dependency '{}' choosing the versions {}",
                groupAndName, chosen)
        return chosen.toString()
    }

    private fun toDependencyId(groupAndName: String, version: String): String {
        return "$groupAndName:$version"
    }

    private fun getCartridgeProperties(propsFile: RegularFile): Properties {
        val props = Properties()
        FileInputStream(propsFile.asFile).use {
            props.load(it)
        }
        return props
    }

    private fun getCartridgeName(props: Properties): String {
        return props["cartridge.name"].toString()
    }

    private fun getLibraryIDs(props: Properties): Set<String> {
        val dependsOnLibs = props["cartridge.dependsOnLibs"].toString()
        return if (dependsOnLibs.isEmpty()) setOf() else dependsOnLibs.split(";").toSet()
    }
}

