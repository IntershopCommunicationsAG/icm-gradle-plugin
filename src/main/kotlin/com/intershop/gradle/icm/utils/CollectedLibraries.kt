/*
 * Copyright 2022 Intershop Communications AG.
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
package com.intershop.gradle.icm.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

/**
 * Resolves the libraries listed in a [LibraryListFile] as artifacts.
 *
 * The entries of the library list file are only known after the task that writes it has been executed,
 * therefore they are added to the configuration in a
 * [Configuration.withDependencies] callback, which is evaluated when the configuration is resolved. The
 * configuration itself is created when this class is instantiated, which must happen at configuration time,
 * so that the consuming task neither accesses the project nor creates a configuration during execution.
 *
 * @param project           the project the configuration is created for, used at configuration time only
 * @param configurationName the name of the configuration that collects the libraries
 * @param libraryListFile   the library list file that contains the dependency IDs of the libraries
 * @constructor Creates the configuration that collects the libraries.
 */
class CollectedLibraries(project: Project,
                         configurationName: String,
                         libraryListFile: Provider<RegularFile>) {

    private val configuration: Configuration

    init {
        configuration = project.configurations.create(configurationName)
        configuration.isTransitive = false
        setupAttributes(project)

        // capture the dependency handler at configuration time
        val dependencyHandler = project.dependencies

        configuration.withDependencies { dependencies ->
            LibraryListFile.read(libraryListFile.get()).entries.forEach {
                dependencies.add(dependencyHandler.create(it))
            }
        }
    }

    /**
     * The resolved libraries, mapped from their target file name to the resolved artifact file.
     *
     * The target file name is
     * '''${dependency.moduleGroup}_${dependency.moduleName}_${dependency.moduleVersion}.${artifact.extension}'''
     *
     * @property resolvedLibraries
     */
    val resolvedLibraries: Provider<Map<String, File>>
        get() = configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            artifacts.associate { artifact -> targetFileNameOf(artifact) to artifact.file }
        }

    private fun targetFileNameOf(artifact: ResolvedArtifactResult): String {
        val componentIdentifier = artifact.id.componentIdentifier
        if (componentIdentifier !is ModuleComponentIdentifier) {
            return artifact.file.name
        }
        return with(componentIdentifier) {
            "${group}_${module}_${version}.${artifact.file.extension}"
        }
    }

    /*
     * Need to configure attributes to avoid:
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
    private fun setupAttributes(project: Project) {
        val objects = project.objects

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
