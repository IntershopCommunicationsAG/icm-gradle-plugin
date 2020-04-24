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

import com.intershop.gradle.icm.extension.CartridgeProject
import com.intershop.gradle.icm.extension.NamedCartridgeProject
import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class CreateSitesFolderNew
        @Inject constructor(projectLayout: ProjectLayout,
                            objectFactory: ObjectFactory,
                            val fsOps: FileSystemOperations): DefaultTask() {

    private val modulesProperty: MapProperty<String, NamedCartridgeProject> =
        objectFactory.mapProperty(String::class.java, NamedCartridgeProject::class.java)
    private val baseProjectProperty: Property<CartridgeProject> = objectFactory.property(CartridgeProject::class.java)

    @get:Internal
    val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    fun provideOutputDir(cartridgeDir: Provider<Directory>) = outputDirProperty.set(cartridgeDir)

    @get:OutputDirectory
    var outputDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    @get:Nested
    var baseProject: CartridgeProject
        get() = baseProjectProperty.get()
        set(value) = baseProjectProperty.set(value)

    @get:Nested
    var modules: Map<String, NamedCartridgeProject>
        get() = modulesProperty.get()
        set(value) = modulesProperty.putAll(value)

    init {
        outputDirProperty.convention(projectLayout.buildDirectory.dir("server/sites"))
    }

    @TaskAction
    fun createSites() {
        val cs = project.copySpec()

        PackageUtil.addPackageToCS(project, baseProject.dependency, "sites", cs, baseProject.sitesPackage)
        modules.forEach { _, prj ->
            PackageUtil.addPackageToCS(project, prj.dependency, "sites", cs, prj.sitesPackage)
        }

        fsOps.copy {
            it.with(cs)
            it.into(outputDir)
        }
    }

}