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

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class DownloadPackage @Inject constructor(
        objectFactory: ObjectFactory,
        private var fsOps: FileSystemOperations) : DefaultTask() {

    private val dependencyProperty: Property<String> = objectFactory.property(String::class.java)
    private val classifierProperty: Property<String> = objectFactory.property(String::class.java)
    private val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
    }

    /**
     * Set provider for dependency configuration.
     *
     * @param dependency dependency provider.
     */
    @Suppress("unused")
    fun provideDependency(dependency: Provider<String>) =
        dependencyProperty.set(dependency)

    /**
     * The dependency of the package.
     *
     * @property dependency package dependency
     */
    @get:Input
    var dependency by dependencyProperty

    /**
     * Set provider for classifier configuration.
     *
     * @param classifier classifier provider.
     */
    @Suppress("unused")
    fun provideClassifier(classifier: Provider<String>) =
        classifierProperty.set(classifier)

    /**
     * The classifier of the package.
     *
     * @property classifier package classifier
     */
    @get:Input
    var classifier by classifierProperty

    /**
     * Provider configuration for target directory.
     *
     * @param provideOutputDir
     */
    fun provideOutputDir(outputDir: Provider<Directory>) = outputDirProperty.set(outputDir)

    /**
     * Output directory for copied files.
     *
     * @property outputDir
     */
    @get:OutputDirectory
    var outputDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    @TaskAction
    fun downloadPackage() {
        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependencyProperty.get()) as ExternalModuleDependency
        dep.artifact {
            it.name = dep.name
            it.classifier = classifierProperty.get()
            it.extension = "zip"
            it.type = "zip"
        }

        val configuration = project.configurations.maybeCreate(configurationName)
        configuration.setVisible(false)
            .setTransitive(false)
            .setDescription("Configuration for package download: ${this.name}")
            .defaultDependencies { ds ->
                ds.add(dep)
            }

        val files = configuration.resolve()
        files.forEach { file ->
            fsOps.copy { cs ->
                cs.from(project.zipTree(file))
                cs.into(outputDir)
            }
        }
    }

    private val configurationName: String
        get() = "${this.name.toLowerCase()}_configuration"
}
