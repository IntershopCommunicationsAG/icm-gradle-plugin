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
import groovy.util.XmlSlurper
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.query.ArtifactResolutionQuery
import org.gradle.api.artifacts.result.ArtifactResolutionResult
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File

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

    @get:Classpath
    private val cartridgeRuntimelist: FileCollection by lazy {
        val returnFiles = project.files()

        if (project.convention.findPlugin(JavaPluginConvention::class.java) != null) {
            returnFiles.from(project.configurations.getByName("cartridgeRuntime").files)
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

        comment = "Intershop descriptor file"

        var cartridges = HashSet<String>()
        var cartridgesTransitive = HashSet<String>()

        project.configurations.getByName("cartridge").allDependencies.forEach { dependensy ->
            if(dependensy is ModuleDependency) {
                cartridges.add(dependensy.name)
            }
        }
        project.configurations.getByName("cartridgeRuntime").allDependencies.forEach { dependensy ->
            if(dependensy is ModuleDependency) {
                cartridges.add(dependensy.name)
            }
        }

        project.configurations.getByName("cartridgeRuntime").resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach {
            it.moduleArtifacts.forEach {
                var identifier = it.id.componentIdentifier
                if(identifier is ProjectComponentIdentifier) {
                    cartridgesTransitive.add(project.project( identifier.projectPath ).name)
                }

                if(identifier is ModuleComponentIdentifier) {
                    if(isCartridge(identifier)) {
                        cartridgesTransitive.add(identifier.module)
                    }
                }
            }
        }

        property("cartridge.dependsOn", cartridges.toSortedSet().joinToString( separator = ";" ))
        property("cartridge.dependsOn.transitive", cartridgesTransitive.toSortedSet().joinToString( separator = ";" ))

        property("cartridge.name", cartridgeName)
        property("cartridge.version", cartridgeVersion)
        property("cartridge.displayName", displayName)
        property("cartridge.description", cartridgeDescription)

        super.writeProperties()
    }

    private fun isCartridge(moduleID : ModuleComponentIdentifier) : Boolean {
        val query: ArtifactResolutionQuery = project.dependencies.createArtifactResolutionQuery()
            .forModule(moduleID.group, moduleID.module, moduleID.version)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)

        val result: ArtifactResolutionResult = query.execute()

        result.resolvedComponents.forEach { component ->
            val mavenPomArtifacts: Set<ArtifactResult> = component.getArtifacts(MavenPomArtifact::class.java)
            val modulePomArtifact =
                mavenPomArtifacts
                    .find { it is ResolvedArtifactResult &&
                            it.file.name == "${moduleID.module}-${moduleID.version}.pom"} as ResolvedArtifactResult

            try {
                val xml = XmlSlurper().parse(modulePomArtifact.file)
                println("found .... " + modulePomArtifact.file.name + " ... " + xml.getProperty("name"))
            }catch (ex: Exception) {
                project.logger.info("Pom file is not readable - " + moduleID.moduleIdentifier)
            }
        }
        return false
    }
}
