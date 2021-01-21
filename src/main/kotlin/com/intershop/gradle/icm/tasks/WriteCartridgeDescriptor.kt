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
import com.intershop.gradle.icm.ICMBasePlugin.Companion.CONFIGURATION_CARTRIDGERUNTIME
import com.intershop.gradle.icm.extension.IntershopExtension.Companion.INTERSHOP_GROUP_NAME
import com.intershop.gradle.icm.utils.CartridgeUtil
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.util.PropertiesUtils
import java.io.File
import java.nio.charset.Charset
import java.util.*
import javax.inject.Inject

/**
 * WriteCartridgeDescriptor Gradle task 'writeCartridgeDescriptor'
 *
 * This task writes a cartridge descriptor file. This file
 * is used by the server startup and special tests.
 */
open class WriteCartridgeDescriptor
        @Inject constructor( projectLayout: ProjectLayout,
                             objectFactory: ObjectFactory ) : DefaultTask() {

    private val outputFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val versionProperty: Property<String> = objectFactory.property(String::class.java)
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
     * Set provider for descriptor version property.
     *
     * @param cartridgeVersion set provider for project version.
     */
    @Suppress( "unused")
    fun provideCartridgeVersion(cartridgeVersion: Provider<String>) = versionProperty.set(cartridgeVersion)

    @get:Input
    var cartridgeVersion: String
        get() = versionProperty.getOrElse(project.version.toString())
        set(value) = versionProperty.set(value)

    /**
     * Set provider for descriptor cartridge name property.
     *
     * @param cartridgeName set provider for cartridge name.
     */
    @Suppress( "unused")
    fun provideCartridgeName(cartridgeName: Provider<String>) = nameProperty.set(cartridgeName)

    @get:Input
    var cartridgeName: String
        get() = nameProperty.get()
        set(value) = nameProperty.set(value)

    /**
     * Set provider for descriptor cartridge description property.
     *
     * @param cartridgeDescription set provider for cartridge description.
     */
    @Suppress( "unused")
    fun provideCartridgeDescription(cartridgeDescription: Provider<String>) =
        descriptionProperty.set(cartridgeDescription)

    @get:Optional
    @get:Input
    var cartridgeDescription: String
        get() = descriptionProperty.getOrElse("")
        set(value) = descriptionProperty.set(value)

    /**
     * Set provider for descriptor display name property.
     *
     * @param displayName set provider for display name.
     */
    @Suppress( "unused")
    fun provideDisplayName(displayName: Provider<String>) = displayNameProperty.set(displayName)

    @get:Input
    var displayName: String
        get() = displayNameProperty.get()
        set(value) = displayNameProperty.set(value)


    @get:Input
    val cartridgeDependencies: List<String> by lazy {
        val returnDeps = mutableListOf<String>()
        project.configurations.getByName(CONFIGURATION_CARTRIDGE).dependencies.forEach {
            returnDeps.add(it.toString())
        }
        returnDeps
    }

    @get:Input
    val cartridgeRuntimeDependencies: List<String> by lazy {
        val returnDeps = mutableListOf<String>()
        project.configurations.getByName(CONFIGURATION_CARTRIDGERUNTIME).dependencies.forEach {
            returnDeps.add(it.toString())
        }
        returnDeps
    }

    /**
     * Provides an output file for this task.
     *
     * @param outputfile
     */
    fun provideOutputfile(outputfile: Provider<RegularFile>) = outputFileProperty.set(outputfile)

    /**
     * Output file for generated cluster id.
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
        if(! outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        val props = linkedMapOf<String,String>()
        val comment = "Intershop descriptor file"

        val cartridges = HashSet<String>()
        val noCartridges = HashSet<String>()
        val cartridgesTransitive = HashSet<String>()

        addCartridges(CONFIGURATION_CARTRIDGE, cartridges)
        addCartridges(CONFIGURATION_CARTRIDGERUNTIME, cartridges)

        addTransitiveCartridges(CONFIGURATION_CARTRIDGERUNTIME, cartridgesTransitive)

        cartridges.forEach {
            if(! cartridgesTransitive.contains(it)) {
                logger.info("It seems, that {} is not a cartridge!", it)
                noCartridges.add(it)
            }
        }

        cartridges.removeAll(noCartridges)

        props["cartridge.dependsOn"] = cartridges.toSortedSet().joinToString( separator = ";" )
        props["cartridge.dependsOn.transitive"] = cartridgesTransitive.toSortedSet().joinToString( separator = ";" )

        props["cartridge.name"] = cartridgeName
        props["cartridge.version"] = cartridgeVersion
        props["cartridge.displayName"] = displayName
        if(! descriptionProperty.isPresent || descriptionProperty.getOrElse("") == "") {
            if(project.description != "") {
                props["cartridge.description"] = project.description ?: project.name
            }
        }

        val propsObject = Properties()
        propsObject.putAll(props)
        try {
            PropertiesUtils.store(
                propsObject,
                outputFile,
                comment,
                Charset.forName("ISO_8859_1"),
                "\n"
            )
        } finally {
            project.logger.debug("Write properties finished not correct.")
        }
    }

    private fun addCartridges(confName: String, cartridges: HashSet<String>) {
        project.configurations.getByName(confName).allDependencies.forEach { dependency ->
            if(dependency is ModuleDependency) {
                cartridges.add(dependency.name)
            }
        }
    }

    private fun addTransitiveCartridges(confName: String, cartridges: HashSet<String>) {
        project.configurations.getByName(confName).
        resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { dependency ->
            dependency.moduleArtifacts.forEach { artifact ->

                val identifier = artifact.id.componentIdentifier
                if(identifier is ProjectComponentIdentifier) {
                    cartridges.add(project.project( identifier.projectPath ).name)
                }

                if(identifier is ModuleComponentIdentifier) {
                    if(CartridgeUtil.isCartridge(project, identifier)) {
                        cartridges.add(identifier.module)
                    }
                }
            }
        }
    }

}
