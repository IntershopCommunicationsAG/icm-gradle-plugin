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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.platform.base.Library
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
                                { _ , p -> if (null == p) at.project.name else p + " " + at.project.name })
                        }
                    })
                }
            }
        }

        val conflicts = deps.filter { e -> 1 < e.value.size }.map { it.value }.toList()
        if (!conflicts.isEmpty()) {
            throw GradleException(
                "Unable to process libraries. Dependencies ${conflicts}" +
                        " are required by cartridge-projects in non-unique versions."
            )
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
        setupConfiguration(configuration)

        // add dependencies
        ids.forEach { configuration.dependencies.add(project.dependencies.create(it)) }

        // process resolved artifacts
        configuration.resolvedConfiguration.getResolvedArtifacts().forEach { artifact ->
            val id = artifact.moduleVersion.id
            libCopySpec.with(project.copySpec().from(artifact.file).rename {
                "${id.group}_" +
                        "${id.name}_" +
                        "${id.version}." +
                        artifact.extension
            })
        }
        return libCopySpec
    }


    /* Need to configure attributes to avoid:

        org.gradle.internal.component.AmbiguousConfigurationSelectionException:
            Cannot choose between the following variants of org.junit.jupiter:junit-jupiter-params:5.7.1:
          - runtimeElements
          - shadowRuntimeElements
        All of them match the consumer attributes:
          - Variant 'runtimeElements' capability org.junit.jupiter:junit-jupiter-params:5.7.1:
              - Unmatched attributes:
                  - Provides org.gradle.category 'library' but the consumer didn't ask for it
                  - Provides org.gradle.dependency.bundling 'external' but the consumer didn't ask for it
                  - Provides org.gradle.jvm.version '8' but the consumer didn't ask for it
                  - Provides org.gradle.libraryelements 'jar' but the consumer didn't ask for it
                  - Provides org.gradle.status 'release' but the consumer didn't ask for it
                  - Provides org.gradle.usage 'java-runtime' but the consumer didn't ask for it
                  - Provides org.jetbrains.kotlin.localToProject 'public' but the consumer didn't ask for it
                  - Provides org.jetbrains.kotlin.platform.type 'jvm' but the consumer didn't ask for it
          - Variant 'shadowRuntimeElements' capability org.junit.jupiter:junit-jupiter-params:5.7.1:
              - Unmatched attributes:
                  - Provides org.gradle.category 'library' but the consumer didn't ask for it
                  - Provides org.gradle.dependency.bundling 'embedded' but the consumer didn't ask for it
                  - Provides org.gradle.jvm.version '8' but the consumer didn't ask for it
                  - Provides org.gradle.libraryelements 'jar' but the consumer didn't ask for it
                  - Provides org.gradle.status 'release' but the consumer didn't ask for it
                  - Provides org.gradle.usage 'java-runtime' but the consumer didn't ask for it
     */
    private fun setupConfiguration(configuration: Configuration) {
        configuration.setTransitive(false)

        /*
           from Java: {
            org.gradle.category=library,
            org.gradle.dependency.bundling=external,
            org.gradle.jvm.environment=standard-jvm,
            org.gradle.jvm.version=11,
            org.gradle.libraryelements=jar,
            org.gradle.usage=java-runtime}
         */
        configuration.attributes { attributeContainer ->
            attributeContainer.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.getObjects().named(Category::class.java, Category.LIBRARY)
            )
            attributeContainer.attribute(
                Bundling.BUNDLING_ATTRIBUTE,
                project.getObjects().named(Bundling::class.java, Bundling.EXTERNAL)
            )
            attributeContainer.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                project.getObjects().named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM)
            )
            attributeContainer.attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                project.getObjects().named(LibraryElements::class.java, LibraryElements.JAR)
            )
            attributeContainer.attribute(
                Usage.USAGE_ATTRIBUTE,
                project.getObjects().named(Usage::class.java, Usage.JAVA_RUNTIME)
            )
        }
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

