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

import com.intershop.gradle.icm.ICMBasePlugin.Companion.CONFIGURATION_CARTRIDGE
import com.intershop.gradle.icm.ICMBasePlugin.Companion.CONFIGURATION_CARTRIDGE_RUNTIME
import com.intershop.gradle.icm.extension.IntershopExtension.Companion.INTERSHOP_GROUP_NAME
import com.intershop.gradle.icm.utils.CartridgeUtil
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.LenientConfiguration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.util.PropertiesUtils
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Properties

import javax.inject.Inject
import kotlin.collections.HashSet

/**
 * WriteCartridgeDescriptor Gradle task 'writeCartridgeDescriptor'
 *
 * This task writes a cartridge descriptor file. This file
 * is used by the server startup and special tests.
 */
open class WriteCartridgeDescriptor
@Inject constructor(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory
) : DefaultTask() {

    private val outputFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val nameProperty: Property<String> = objectFactory.property(String::class.java)
    private val descriptionProperty: Property<String> = objectFactory.property(String::class.java)
    private val displayNameProperty: Property<String> = objectFactory.property(String::class.java)

    companion object {
        const val DEFAULT_NAME = "writeCartridgeDescriptor"
        const val CARTRIDGE_DESCRIPTOR = "descriptor/cartridge.descriptor"
    }

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Writes all necessary information of the cartridge to a file."

        outputFileProperty.convention(projectLayout.buildDirectory.file(CARTRIDGE_DESCRIPTOR))

        nameProperty.convention(project.name)
        displayNameProperty.convention(project.description ?: project.name)
    }

    /**
     * Set provider for descriptor cartridge name property.
     *
     * @param cartridgeName set provider for cartridge name.
     */
    @Suppress("unused")
    fun provideCartridgeName(cartridgeName: Provider<String>) = nameProperty.set(cartridgeName)

    @get:Input
    var cartridgeName: String
        get() = nameProperty.get()
        set(value) = nameProperty.set(value)

    /**
     * Set provider for descriptor display name property.
     *
     * @param displayName set provider for display name.
     */
    @Suppress("unused")
    fun provideDisplayName(displayName: Provider<String>) = displayNameProperty.set(displayName)

    @get:Input
    var displayName: String
        get() = displayNameProperty.get()
        set(value) = displayNameProperty.set(value)

    @get:Input
    val cartridgeDependencies: String by lazy {
        flattenToString(
            { project.configurations.getByName(CONFIGURATION_CARTRIDGE).dependencies },
            { value ->
                value.toString().apply {
                    project.logger.debug("CartridgeDependencies of project {}: {}", project.name, this)
                }
            }
        )
    }

    @get:Input
    val runtimeDependencies: String by lazy {
        flattenToString(
            {
                project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME).resolvedConfiguration
                    .lenientConfiguration.allModuleDependencies
            },
            { value ->
                value.toString().apply {
                    project.logger.debug("RuntimeDependencies of project {}: {}", project.name, this)
                }
            }
        )
    }

    /**
     * Provides an output file for this task.
     *
     * @param outputfile
     */
    fun provideOutputfile(outputfile: Provider<RegularFile>) = outputFileProperty.set(outputfile)

    /**
     * Output file for generated descriptor.
     *
     * @property outputFile
     */
    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)

    /**
     * Task method for the creation of a descriptor file.
     */
    @TaskAction
    fun runFileCreation() {
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        val props = linkedMapOf<String, String>()
        val comment = "Intershop descriptor file"

        val cartridges = getCartridges()
        val libs = getLibs()

        props["descriptor.version"] = "1.0"

        props["cartridge.dependsOn"] = cartridges.toSortedSet().joinToString(separator = ";")
        props["cartridge.dependsOnLibs"] = libs.toSortedSet().joinToString(separator = ";")

        props["cartridge.name"] = cartridgeName
        props["cartridge.displayName"] = displayName
        props["cartridge.description"] = displayName
        props["cartridge.version"] = project.version.toString()

        if (project.hasProperty("cartridge.style")) {
            props["cartridge.style"] = project.property("cartridge.style").toString()
        }

        val propsObject = Properties()
        propsObject.putAll(props)
        try {
            PropertiesUtils.store(
                propsObject,
                outputFile,
                comment,
                StandardCharsets.ISO_8859_1,
                "\n"
            )
        } finally {
            project.logger.debug("Wrote cartridge descriptor.")
        }
    }

    @Internal
    fun getLibraryIDs(): Set<String> {
        val props = Properties()
        FileInputStream(outputFile).use {
            props.load(it)
        }
        val dependsOnLibs = props["cartridge.dependsOnLibs"].toString()
        return if (dependsOnLibs.isEmpty()) setOf() else dependsOnLibs.split(";").toSet()
    }

    private fun getLibs(): Set<String> {
        val dependencies = HashSet<String>()
        val lenientConfiguration = project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            .resolvedConfiguration.lenientConfiguration
        logUnresolvedDepencencies(lenientConfiguration)
        lenientConfiguration.allModuleDependencies.forEach { dependency ->
                dependency.moduleArtifacts.forEach { artifact ->
                    when (val identifier = artifact.id.componentIdentifier) {
                        is ModuleComponentIdentifier -> {
                            // only add non-cartridge-'jar's
                            if (artifact.extension.equals("jar") && !CartridgeUtil.isCartridge(project, identifier)) {
                                dependencies.add("${identifier.group}:${identifier.module}:${identifier.version}")
                            }
                        }
                    }
                }
            }
        return dependencies
    }

    private fun getCartridges(): Set<String> {
        val dependencies = HashSet<String>()
        val lenientConfiguration = project.configurations.getByName(CONFIGURATION_CARTRIDGE_RUNTIME)
            .resolvedConfiguration.lenientConfiguration
        logUnresolvedDepencencies(lenientConfiguration)
        lenientConfiguration.allModuleDependencies.forEach { dependency ->
                dependency.moduleArtifacts.forEach { artifact ->
                    when (val identifier = artifact.id.componentIdentifier) {
                        is ProjectComponentIdentifier ->
                            dependencies.add(identifier.projectName)
                        is ModuleComponentIdentifier ->
                            if (CartridgeUtil.isCartridge(project, identifier)) {
                                dependencies.add("${identifier.module}:${identifier.version}")
                            }
                    }
                }
            }
        return dependencies
    }

    private fun logUnresolvedDepencencies(lenientConfiguration: LenientConfiguration) {
        val unresolvedModuleDependencies = lenientConfiguration.unresolvedModuleDependencies
        if (unresolvedModuleDependencies.isNotEmpty()){
            project.logger.warn("Failed to resolve the following dependencies while writing the cartridge descriptor for project {}: {}", project.name, unresolvedModuleDependencies)
        }
    }

    private fun <E> flattenToString(
        collectionProvider: () -> Collection<E>,
        stringifier: (value: E) -> String = { value -> value.toString() }
    ): String =
        collectionProvider.invoke().map { value -> stringifier.invoke(value) }.sorted().toString()

}

