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
import com.intershop.gradle.icm.extension.FolderConfig
import com.intershop.gradle.icm.extension.NamedCartridgeProject
import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
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
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class CreateSitesFolder
        @Inject constructor(val projectLayout: ProjectLayout,
                            val objectFactory: ObjectFactory,
                            val fsOps: FileSystemOperations): DefaultTask() {

    @get:Internal
    val outputDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    fun provideOutputDir(cartridgeDir: Provider<Directory>) = outputDirProperty.set(cartridgeDir)

    @get:OutputDirectory
    var outputDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    @get:Nested
    var baseProject: Property<CartridgeProject> = objectFactory.property(CartridgeProject::class.java)

    @get:Nested
    var modules: MapProperty<String, NamedCartridgeProject> =
                    objectFactory.mapProperty(String::class.java, NamedCartridgeProject::class.java)

    @get:Optional
    @get:Nested
    val baseFolderConfig: Property<FolderConfig> = objectFactory.property(FolderConfig::class.java)

    @get:Optional
    @get:Nested
    val extraFolderConfig: Property<FolderConfig> = objectFactory.property(FolderConfig::class.java)

    init {
        outputDirProperty.convention(projectLayout.buildDirectory.dir("server/sites"))
    }

    @TaskAction
    fun createSites() {
        val cs = project.copySpec()

        PackageUtil.addPackageToCS(project, baseProject.get().dependency, "sites", cs, baseProject.get().sitesPackage)
        modules.get().forEach { _, prj ->
            PackageUtil.addPackageToCS(project, prj.dependency, "sites", cs, prj.sitesPackage)
        }

        if(baseFolderConfig.isPresent) {
            cs.with(getCSFolderConfig(baseFolderConfig.get()))
        }

        if(extraFolderConfig.isPresent) {
            cs.with(getCSFolderConfig(extraFolderConfig.get()))
        }

        fsOps.copy {
            it.with(cs)
            it.into(outputDir)
        }
    }

    private fun getCSFolderConfig(folder: FolderConfig): CopySpec {
        val fcCS = project.copySpec()

        fcCS.from(folder.dir)

        folder.excludes.get().forEach {
            fcCS.exclude(it)
        }

        folder.includes.get().forEach {
            fcCS.include(it)
        }

        if(folder.target.isPresent) {
            fcCS.into(folder.target.get())
        }

        return fcCS
    }

}