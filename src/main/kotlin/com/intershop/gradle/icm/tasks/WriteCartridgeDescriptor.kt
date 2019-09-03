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
import com.intershop.gradle.icm.getValue
import com.intershop.gradle.icm.setValue
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

/**
 * WriteCartridgeDescriptor Gradle task 'writeCartridgeDescriptor'
 *
 * This task writes a cartridge descriptor file. This file
 * is used by the server startup and special tests.
 */
open class WriteCartridgeDescriptor : WriteProperties() {

    private val versionProperty: Property<String> = project.objects.property(String::class.java)
    private val nameProperty: Property<String> = project.objects.property(String::class.java)
    private val descritpionProperty: Property<String> = project.objects.property(String::class.java)
    private val displayNameProperty: Property<String> = project.objects.property(String::class.java)

    init {
        outputFile = File(project.buildDir,
            "${ICMBuildPlugin.CARTRIDGE_DESCRIPTOR_DIR}/${ICMBuildPlugin.CARTRIDGE_DESCRIPTOR_FILE}")
        versionProperty.set(project.version.toString())
        nameProperty.set(project.name)
        descritpionProperty.set(
            if (project.description != null && project.description.toString().length > 0)
                project.description
            else
                project.name)
        displayNameProperty.set(project.name)
    }

    /**
     * Set provider for descriptor version property.
     *
     * @param cartridgeVersion set provider for project version.
     */
    @Suppress( "unused")
    fun provideCartridgeVersion(cartridgeVersion: Provider<String>) = versionProperty.set(cartridgeVersion)

    @get:Input
    var cartridgeVersion by versionProperty

    /**
     * Set provider for descriptor cartridge name property.
     *
     * @param cartridgeName set provider for cartridge name.
     */
    @Suppress( "unused")
    fun provideCartridgeName(cartridgeName: Provider<String>) = nameProperty.set(cartridgeName)

    @get:Input
    var cartridgeName by nameProperty

    /**
     * Set provider for descriptor cartridge description property.
     *
     * @param cartridgeDescription set provider for cartridge description.
     */
    @Suppress( "unused")
    fun provideCartridgeDescription(cartridgeDescription: Provider<String>) =
        descritpionProperty.set(cartridgeDescription)

    @get:Input
    var cartridgeDescription by descritpionProperty

    /**
     * Set provider for descriptor display name property.
     *
     * @param displayName set provider for display name.
     */
    @Suppress( "unused")
    fun provideDisplayName(displayName: Provider<String>) = displayNameProperty.set(displayName)

    @get:Input
    var displayName by displayNameProperty

    @get:Classpath
    private val cartridgelist: FileCollection by lazy {
        val returnFiles = project.files()

        if (project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            returnFiles.from(project.configurations.getByName("cartridge").files)
        }

        returnFiles
    }

    /**
     * Task method for the creation of a descriptor file.
     */
    @Suppress("unused")
    @TaskAction
    fun runFileCreation() {
        if(! outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        ICMBuildPlugin.addCartridgeDependenciesProps(project)

        comment = "Intershop descriptor file"

        var cartridgesDependsOn = project.extensions.extraProperties.get("cartridges.dependsOn")
        var cartridgestTransitivDependsOn = project.extensions.extraProperties.get("cartridges.transitive.dependsOn")

        if(cartridgesDependsOn is List<*>) {
            property("cartridge.dependsOn", cartridgesDependsOn.joinToString(separator = ";"))
        } else {
            property("cartridge.dependsOn", if( cartridgesDependsOn != null) cartridgesDependsOn else "")
        }

        if(cartridgestTransitivDependsOn is List<*>) {
            property("cartridge.dependsOn.transitive", cartridgestTransitivDependsOn.joinToString(separator = ";"))
        } else {
            property("cartridge.dependsOn.transitive",
                if( cartridgestTransitivDependsOn != null)
                    cartridgestTransitivDependsOn
                else
                    "")
        }

        property("cartridge.name", cartridgeName)
        property("cartridge.version", cartridgeVersion)
        property("cartridge.displayName", displayName)
        property("cartridge.description", cartridgeDescription)

        super.writeProperties()
    }
}
