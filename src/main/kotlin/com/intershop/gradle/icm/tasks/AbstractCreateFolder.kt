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
import com.intershop.gradle.icm.extension.ServerDir
import com.intershop.gradle.icm.utils.CopySpecUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
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
import java.io.File
import javax.inject.Inject

abstract class AbstractCreateFolder
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
    val baseProject: Property<CartridgeProject> = objectFactory.property(CartridgeProject::class.java)

    @get:Nested
    val modules: MapProperty<String, NamedCartridgeProject> =
                    objectFactory.mapProperty(String::class.java, NamedCartridgeProject::class.java)

    @get:Optional
    @get:Nested
    val baseDirConfig: Property<ServerDir> = objectFactory.property(ServerDir::class.java)

    @get:Optional
    @get:Nested
    val extraDirConfig: Property<ServerDir> = objectFactory.property(ServerDir::class.java)

    protected fun createFolder() {
        val cs = project.copySpec()
        cs.duplicatesStrategy = DuplicatesStrategy.FAIL

        addPackages(cs)

        if(baseDirConfig.isPresent && baseDirConfig.get().dirs.isNotEmpty()) {
            cs.with(CopySpecUtil.getCSForServerDir(project, baseDirConfig.get()))
        }

        if(extraDirConfig.isPresent && extraDirConfig.get().dirs.isNotEmpty()) {
            cs.with(CopySpecUtil.getCSForServerDir(project, extraDirConfig.get()))
        }

        fsOps.copy {
            it.with(cs)
            it.into(outputDir)
        }
    }

    abstract fun addPackages(cs: CopySpec)

}