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
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.util.PropertiesUtils
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

    companion object {
        const val DEFAULT_NAME = "writeCartridgeDescriptor"
        const val CARTRIDGE_DESCRIPTOR = "descriptor/cartridge.descriptor"
    }

    /**
     * Set property for descriptor cartridge name property.
     *
     * @param cartridgeName set provider for cartridge name.
     */
    @get:Input
    val cartridgeName: Property<String> = objectFactory.property(String::class.java)

    /**
     * Set property for display cartridge name property.
     *
     * @param cartridgeName set provider for cartridge name.
     */
    @get:Input
    val displayName: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    val cartridgeVersion: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    val cartridgeStyle: Property<String> = objectFactory.property(String::class.java)

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
     * Output file for generated descriptor.
     *
     * @property outputFile
     */
    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Writes all necessary information of the cartridge to a file."

        outputFile.convention(projectLayout.buildDirectory.file(CARTRIDGE_DESCRIPTOR))

        cartridgeName.convention(project.name)
        displayName.convention(project.description ?: project.name)
        cartridgeVersion.convention(project.version.toString())

        cartridgeStyle.convention(CartridgeUtil.getCartridgeStyle(project).name)

    }

    /**
     * Task method for the creation of a descriptor file.
     */
    @TaskAction
    fun runFileCreation() {
        if (!outputFile.asFile.get().exists()) {
            outputFile.asFile.get().parentFile.mkdirs()
        }

        val props = linkedMapOf<String, String>()
        val comment = "Intershop descriptor file"

        val cartridgeDependencies = getCartridgeDependencies()
        val libs = getLibs(cartridgeDependencies)

        props["descriptor.version"] = "1.0"

        props["cartridge.dependsOn"] = cartridgeDependencies.map { it.cartridgeId }.toSortedSet()
                .joinToString(separator = ";")
        props["cartridge.dependsOnLibs"] = libs.toSortedSet().joinToString(separator = ";")

        props["cartridge.name"] = cartridgeName.get()
        props["cartridge.displayName"] = displayName.get()
        props["cartridge.description"] = displayName.get()
        props["cartridge.version"] = cartridgeVersion.get()

        if(cartridgeStyle.get().isNotBlank()) {
            props["cartridge.style"] = cartridgeStyle.get()
        }

        val propsObject = Properties()
        propsObject.putAll(props)
        try {
            PropertiesUtils.store(
                propsObject,
                outputFile.asFile.get(),
                comment,
                StandardCharsets.ISO_8859_1,
                "\n"
            )
        } finally {
            project.logger.debug("Wrote cartridge descriptor.")
        }
    }

    /**
     * Reimplementation of [ResolvedDependency.getAllModuleArtifacts] but with circle detection (already processed
     * dependencies will be skipped).
     * Original method [ResolvedDependency.getAllModuleArtifacts] will get stuck in a stack overflow when processing
     * `org.apache.solr:solr-solrj` which is dependent from `org.apache.solr:solr-solrj-zookeeper` and vice versa.
     */
    private fun getAllModuleArtifacts(
            dependency: ResolvedDependency,
            processedDependencies: MutableSet<ResolvedDependency>,
    ): Set<ResolvedArtifact> {
        project.logger.debug("Determining module artifacts of {} transitively", dependency.name)
        // detect circular dependencies like
        if (processedDependencies.contains(dependency)) {
            project.logger.debug("Dependency {} already has been processed", dependency.name)
            return setOf() // no extra artifacts
        }
        processedDependencies.add(dependency) // mark as processed
        var artifacts = dependency.moduleArtifacts // own artifacts
        project.logger.debug("Found artifacts {}", artifacts)
        dependency.children.forEach { child ->
            artifacts = artifacts + getAllModuleArtifacts(child, processedDependencies) // append child artifacts
        }
        return artifacts
    }

    private fun getLibs(cartridgeDependencies: Set<CartridgeDependency>): Set<String> {
        val processedDependencies = mutableSetOf<ResolvedDependency>()
        // put the ids of all cartridge dependencies into 1 single set for later lookup
        val derivedLibraryDependencyIds = cartridgeDependencies.flatMap {
            cartDep ->
            cartDep.childDependencies.flatMap { childDep -> // 1. get children
                getAllModuleArtifacts(childDep, processedDependencies) // 2. get artifacts of children (transitive!)
            }
        }.asSequence().filter { it.extension.equals("jar") } // 3. jars only
                .map { it.id.componentIdentifier } // 4. get componentIdentifier
                .filter { it is ModuleComponentIdentifier } // 5. ModuleComponentIdentifiers only
                .map {
                    with(it as ModuleComponentIdentifier) { // cast to ModuleComponentIdentifier
                        "${group}:${module}:${version}" // 4. render actual id
                    }
                }.toSet()

        val dependencies = HashSet<String>()
        val resolvedConfig = project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
                .resolvedConfiguration
        // ensure build fails if there are resolve errors
        if (resolvedConfig.hasError()){
            resolvedConfig.rethrowFailure()
        }

        var transitiveCount = 0
        var directCount = 0
        resolvedConfig.lenientConfiguration.allModuleDependencies.forEach { dependency ->
            dependency.moduleArtifacts.forEach { artifact ->
                when (val identifier = artifact.id.componentIdentifier) {
                    is ModuleComponentIdentifier -> {
                        // only add non-cartridge-'jar's
                        if (artifact.extension.equals("jar") && !CartridgeUtil.isCartridge(project, identifier)) {
                            val id = "${identifier.group}:${identifier.module}:${identifier.version}"
                            transitiveCount++
                            // only return libs that haven't come along with other cartridges
                            if (!derivedLibraryDependencyIds.contains(id)) {
                                dependencies.add(id)
                                directCount++
                            }
                        }
                    }
                }
            }
        }
        project.logger.debug("Cartridge {} directly depends on {} libraries and transitively on {}",
                project.name, directCount, transitiveCount)
        return dependencies
    }

    private fun getCartridgeDependencies(): Set<CartridgeDependency> {
        val dependencies = HashSet<CartridgeDependency>()
        val resolvedConfig = project.configurations.getByName(CONFIGURATION_CARTRIDGE_RUNTIME)
                .resolvedConfiguration
        // ensure build fails if there are resolve errors
        if (resolvedConfig.hasError()){
            resolvedConfig.rethrowFailure()
        }
        resolvedConfig.lenientConfiguration.firstLevelModuleDependencies.forEach { dependency ->
            dependency.moduleArtifacts.forEach { artifact ->
                when (val identifier = artifact.id.componentIdentifier) {
                    is ProjectComponentIdentifier ->
                        dependencies.add(CartridgeDependency(identifier.projectName, dependency.children))
                    is ModuleComponentIdentifier ->
                        if (CartridgeUtil.isCartridge(project, identifier)) {
                            dependencies.add(CartridgeDependency("${identifier.module}:${identifier.version}",
                                    dependency.children))
                        }
                }
            }
        }
        return dependencies
    }

    private fun <E> flattenToString(
        collectionProvider: () -> Collection<E>,
        stringifier: (value: E) -> String = { value -> value.toString() }
    ): String =
        collectionProvider.invoke().map { value -> stringifier.invoke(value) }.sorted().toString()

    private class CartridgeDependency(val cartridgeId : String, val childDependencies : Set<ResolvedDependency>) {
        override fun toString(): String {
            return "CartridgeDependency(cartridgeId='$cartridgeId', childDependencies=$childDependencies)"
        }
    }
}
