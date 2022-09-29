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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Collects all libraries (recursively through all (sub-)projects)
 */
open class CopyLibraries @Inject constructor( objectFactory: ObjectFactory ) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "CopyLibraries"

        fun getName(type: String): String {
            return "${type.lowercase()}${DEFAULT_NAME}"
        }
        fun getOutputPath(type: String): String {
            return "libraries/${type.lowercase()}"
        }
    }

    init {
        group = "ICM server build"
        description = "Copy libraries from a list of dependencies for an environment"
    }

    @get:Input
    val environmentType: Property<String> = objectFactory.property(String::class.java)

    @get:InputFile
    val dependencyIDFile: RegularFileProperty = objectFactory.fileProperty()

    @get:OutputDirectory
    val librariesDirectory: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Task action starts the java process in the background.
     */
    @TaskAction
    fun execute() {
        val libraryDependencyIds = DependencyListUtil.getIDList(environmentType.get(),
                                                                dependencyIDFile.get())
        project.sync {
            it.with(copySpecFor(libraryDependencyIds))
            it.into(librariesDirectory)
        }
    }


    /**
     * Creates a ```CopySpec``` which describes that libraries get copied into the folder
     * '''libraries/{environmentName}''' using file-name
     * '''${dependency.moduleGroup}_${dependency.moduleName}_${dependency.moduleVersion}.${artifact.extension}'''
     */
    private fun copySpecFor(ids: Collection<String>): CopySpec {
        with(project) {
            val libCopySpec = copySpec()

            val configuration = project.configurations.create("CollectedLibraries${environmentType.get()}")
            setupConfiguration(configuration)

            // add dependencies
            ids.forEach { configuration.dependencies.add(project.dependencies.create(it)) }

            // process resolved artifacts
            configuration.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                val id = artifact.moduleVersion.id
                libCopySpec.with(project.copySpec().from(artifact.file).rename {
                    "${id.group}_${id.name}_${id.version}.${artifact.extension}"
                })
            }
            return libCopySpec
        }
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
        configuration.isTransitive = false
        with(project) {
            configuration.attributes { attributeContainer ->
                attributeContainer.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category::class.java, Category.LIBRARY)
                )
                attributeContainer.attribute(
                    Bundling.BUNDLING_ATTRIBUTE,
                    objects.named(Bundling::class.java, Bundling.EXTERNAL)
                )
                attributeContainer.attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM)
                )
                attributeContainer.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements::class.java, LibraryElements.JAR)
                )
                attributeContainer.attribute(
                    Usage.USAGE_ATTRIBUTE,
                    objects.named(Usage::class.java, Usage.JAVA_RUNTIME)
                )
            }
        }
    }
}

