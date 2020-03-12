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

import com.intershop.gradle.icm.extension.BaseProjectConfiguration
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.CreateConfFolder.Companion.CLUSTER_CONF
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import javax.inject.Inject

abstract class AbstractCreateFolder @Inject constructor(
    objectFactory: ObjectFactory,
    private var fsOps: FileSystemOperations): DefaultTask() {

    val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    protected val baseProjectsProperty: MapProperty<String, BaseProjectConfiguration> =
        objectFactory.mapProperty(String::class.java, BaseProjectConfiguration::class.java)

    protected val baseCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)
    protected val devCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)

    fun provideBaseCopySpec(confCopySpec: Provider<CopySpec>) = baseCopySpecProperty.set(confCopySpec)
    fun provideDevCopySpec(confCopySpec: Provider<CopySpec>) = devCopySpecProperty.set(confCopySpec)

    @set:Input
    @set:Nested
    var baseProjects: Map<String, BaseProjectConfiguration>
        get() = baseProjectsProperty.get()
        set(value) = baseProjectsProperty.putAll(value)

    /**
     * Provider configuration for target directory.
     *
     * @param provideOutputDir
     */
    fun provideOutputDir(outputDir: Provider<Directory>) = outputDirProperty.set(outputDir)

    /**
     * Output directory for generated files.
     *
     * @property outputDir
     */
    @get:OutputDirectory
    var outputDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    /**
     * Additional project configuration directory.
     *
     * @property confCopySpec
     */
    @get:Nested
    @get:Input
    var baseCopySpec: CopySpec
        get() = baseCopySpecProperty.get()
        set(value) = baseCopySpecProperty.set(value)

    /**
     * Additional project configuration directory.
     *
     * @property confCopySpec
     */
    @get:Optional
    @get:Nested
    @get:Input
    var devCopySpec: CopySpec?
        get() = devCopySpecProperty.orNull
        set(value) = devCopySpecProperty.set(value)

    @get:Internal
    abstract val classifier: String

    protected fun startFolderCreation() {
        val cs = project.copySpec()

        if(baseCopySpecProperty.isPresent) {
            cs.with(baseCopySpec)
        }
        if(devCopySpecProperty.isPresent) {
            cs.with(devCopySpec)
        }

        baseProjects.forEach {
            val file = downloadPackage(it.value.dependency, classifier)

            if(it.value.withCartridgeList == true) {
                val propsFile = getCartridgeListProps(file)
                if(propsFile != null) {
                    cs.from(propsFile) { cs ->
                        cs.into(CLUSTER_CONF)
                    }
                }
            }

            cs.from(project.zipTree(file))
            addConfySpec(cs, it.value)
        }

        fsOps.copy {
            it.with(cs)
            it.into(outputDir)
        }
    }

    abstract fun addConfySpec(cs: CopySpec, prjConf: BaseProjectConfiguration)

    abstract fun getCartridgeListProps(zipFile: File): File?

    protected fun downloadPackage(dependency: String, classifier: String): File {
        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependency) as ExternalModuleDependency
        dep.artifact {
            it.name = dep.name
            it.classifier = classifier
            it.extension = "zip"
            it.type = "zip"
        }

        val configuration = project.configurations.maybeCreate(configurationName)
        configuration.setVisible(false)
            .setTransitive(false)
            .setDescription("${classifier} for package download: ${this.name}")
            .defaultDependencies { ds ->
                ds.add(dep)
            }

        val files = configuration.resolve()
        return files.first()
    }

    private val configurationName: String
        get() = "${this.name.toLowerCase()}_configuration"
}
