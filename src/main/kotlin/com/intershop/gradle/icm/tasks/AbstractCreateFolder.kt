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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import javax.inject.Inject

/**
 * Base class for folder creation tasks.
 *
 * @property projectLayout service object for project layout handling
 * @property objectFactory service object for object handling
 * @property fsOps service object for file system operations
 * @constructor Creates a task for folder handling.
 */
abstract class AbstractCreateFolder
        @Inject constructor(@Internal val projectLayout: ProjectLayout,
                            @Internal val objectFactory: ObjectFactory,
                            @Internal val fsOps: FileSystemOperations): DefaultTask() {

    /**
     * Provides the output dir of this task.
     *
     * @param cartridgeDir directory provider
     */
    fun provideOutputDir(cartridgeDir: Provider<Directory>) = outputDir.set(cartridgeDir)

    @get:OutputDirectory
    val outputDir: DirectoryProperty = objectFactory.directoryProperty()

    @get:Nested
    val baseProject: Property<CartridgeProject> = objectFactory.property(CartridgeProject::class.java)

    @get:Nested
    val modules: SetProperty<NamedCartridgeProject> = objectFactory.setProperty(NamedCartridgeProject::class.java)

    /**
     * Add a module configuration.
     *
     * @param cartridgeProject a new NamedCartridgeProject configuration
     */
    fun module(cartridgeProject: NamedCartridgeProject) {
        modules.add(cartridgeProject)
    }

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

        cs.exclude("**/**/cartridgelist.properties")
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

    /**
     * Adds packages to a copy spec, so that the files be stored in the output dir.
     *
     * @param cs copy spec will be executed by this task.
     */
    abstract fun addPackages(cs: CopySpec)
}
