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

open class WriteCartridgeDescriptor : WriteProperties() {

    private val versionProperty: Property<String> = project.objects.property(String::class.java)
    private val nameProperty: Property<String> = project.objects.property(String::class.java)
    private val descritpionProperty: Property<String> = project.objects.property(String::class.java)
    private val displayNameProperty: Property<String> = project.objects.property(String::class.java)

    init {
        outputFile = File(project.buildDir, "${ICMBuildPlugin.CARTRIDGE_DESCRIPTOR_DIR}/${ICMBuildPlugin.CARTRIDGE_DESCRIPTOR_FILE}")
        versionProperty.set(project.version.toString())
        nameProperty.set(project.name)
        descritpionProperty.set(if (project.description != null && project.description.toString().length > 0) project.description else project.name)
        displayNameProperty.set(project.name)
    }

    fun provideCartridgeVersion(cartridgeVersion: Provider<String>) = versionProperty.set(cartridgeVersion)

    @get:Input
    var cartridgeVersion by versionProperty

    fun provideCartridgeName(cartridgeName: Provider<String>) = nameProperty.set(cartridgeName)

    @get:Input
    var cartridgeName by nameProperty

    fun provideCartridgeDescription(cartridgeDescription: Provider<String>) = descritpionProperty.set(cartridgeDescription)

    @get:Input
    var cartridgeDescription by descritpionProperty

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
            property("cartridge.transitive.dependsOn", cartridgestTransitivDependsOn.joinToString(separator = ";"))
        } else {
            property("cartridge.transitive.dependsOn", if( cartridgestTransitivDependsOn != null) cartridgestTransitivDependsOn else "")
        }

        property("cartridge.name", cartridgeName)
        property("cartridge.version", cartridgeVersion)
        property("cartridge.displayName", displayName)
        property("cartridge.description", cartridgeDescription)

        super.writeProperties()
    }


}