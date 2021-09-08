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

import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.tooling.model.Dependency
import java.util.function.BiFunction

/**
 * Collects all libraries (recursively through all (sub-)projects)
 */
open class CollectLibraries : DefaultTask() {
    private val copiedLibrariesDirectoryProperty: Property<Directory> =
        project.objects.directoryProperty().convention(project.layout.buildDirectory.dir(BUILD_FOLDER))

    companion object {
        const val DEFAULT_NAME = "collectLibraries"
        const val BUILD_FOLDER = "libraries"
    }

    init {
        group = "ICM server build"
        description = "Collects all libraries (recursively through all (sub-)projects)"
    }

    @get:Input
    val allDependencyIds: String by lazy {
        libraryDependencyIds.toString()
    }

    @get:Internal
    val libraryDependencyIds: Map<EnvironmentType, List<String>> by lazy {
        val dependencies = mutableMapOf<EnvironmentType, MutableSet<String>>()

        val deps = mutableMapOf<String, MutableMap<String, String>>()

        dependsOn.forEach { t ->
            val p = t as TaskProvider<*>
            when (val at = p.get()) {
                is WriteCartridgeDescriptor -> {
                    val environmentType = CartridgeUtil.getCartridgeStyle(at.project).environmentType()
                    dependencies.computeIfAbsent(environmentType, { linkedSetOf() }).addAll(at.getLibraryIDs().apply {
                        this.forEach {
                            val groupAndName = it.substringBeforeLast(':')
                            val versionInProjects = deps.computeIfAbsent(groupAndName, { mutableMapOf() })
                            versionInProjects.compute(
                                it,
                                { k, p -> if (null == p) at.project.name else p + " " + at.project.name })
                        }
                    })
                }
            }
        }

        val conflicts = deps.filter { e -> 1 < e.value.size }.map { it.value }.toList()
        if (!conflicts.isEmpty()) {
            throw GradleException("Unable to process libraries. Dependencies ${conflicts} are required by cartridge-projects in non-unique versions.")
        }

        // ensure ids for a certain EnvironmentType only contain ids of this type not others
        // (e.g. test without production)
        val alreadyThere = mutableSetOf<String>()
        EnvironmentType.values().forEach { env ->
            dependencies.get(env)?.run {
                this.removeAll(alreadyThere)
                alreadyThere.addAll(this)
            }
        }

        val dependencyWithList = mutableMapOf<EnvironmentType, List<String>>()
        dependencies.forEach { (k, v) -> dependencyWithList.put(k, v.toList().sorted()) }

        dependencyWithList.toSortedMap()
    }

    @get:OutputDirectory
    var copiedLibrariesDirectory: Directory
        get() = copiedLibrariesDirectoryProperty.get()
        set(value) = copiedLibrariesDirectoryProperty.set(value)

    /**
     * Task action starts the java process in the background.
     */
    @TaskAction
    fun execute() {
        copiedLibrariesDirectory.asFile.deleteRecursively()
        libraryDependencyIds.map { (key, value) ->
            copySpecFor(key, value)
        }.forEach { copySpec ->
            project.copy { cs -> cs.with(copySpec).into(copiedLibrariesDirectory) }
        }
    }

    /**
     * Creates a ```CopySpec``` which describes that libraries get copied into the folder
     * '''libraries/{environmentName}''' using file-name
     * '''${dependency.moduleGroup}_${dependency.moduleName}_${dependency.moduleVersion}.${artifact.extension}'''
     */
    private fun copySpecFor(environmentType: EnvironmentType, ids: Collection<String>): CopySpec {

        val libCopySpec = project.copySpec().into(environmentType.name.lowercase())
        val configuration = project.configurations.create("CollectedLibraries${environmentType.name}")

        configuration.setTransitive(false)
        ids.forEach { configuration.dependencies.add(project.dependencies.create(it)) }

        configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { dependency ->

            dependency.moduleArtifacts.forEach { artifact ->
                when (artifact.id.componentIdentifier) {
                    is ModuleComponentIdentifier -> {
                        libCopySpec.with(project.copySpec().from(artifact.file).rename { name ->
                            "${dependency.moduleGroup}_" +
                                    "${dependency.moduleName}_" +
                                    "${dependency.moduleVersion}." +
                                    artifact.extension
                        })
                    }
                }
            }
        }

        return libCopySpec
    }

    /**
     * Creates a ```CopySpec``` with
     * + ```from``` = ```copiedLibrariesDirectory```
     * + ```include``` = <matching ```environmentType```>
     * + ```to``` = ```"lib"```
     */
    open fun copySpecFor(environmentType: EnvironmentType): CopySpec =
        project.copySpec { cp ->
            cp.from(copiedLibrariesDirectory.dir(environmentType.name.lowercase())).into("lib")
        }

}

