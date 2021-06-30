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

import com.intershop.gradle.icm.extension.ServerDir
import com.intershop.gradle.icm.utils.CopySpecUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * This task copy the files (configuration) for publishing of ICM projects.
 * There is a configuration for new base projects like ICM or adapter cartridges like (solr).
 *
 * @constructor Creates a task for file copienf from extension.
 */
open class PreparePublishDir @Inject constructor(objectFactory: ObjectFactory,
                                            @Internal var projectLayout: ProjectLayout,
                                            @Internal var fsOps: FileSystemOperations): DefaultTask() {

    @get:Optional
    @get:Nested
    val baseDirConfig: Property<ServerDir> = objectFactory.property(ServerDir::class.java)

    @get:Optional
    @get:Nested
    val extraDirConfig: Property<ServerDir> = objectFactory.property(ServerDir::class.java)

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Task execution method of this task. It creates a new directory.
     */
    @TaskAction
    fun prepareDir() {
        val cs = project.copySpec()
        cs.duplicatesStrategy = DuplicatesStrategy.FAIL

        if(baseDirConfig.get().dirs.isNotEmpty()) {
            cs.with(CopySpecUtil.getCSForServerDir(project, baseDirConfig.get()))
        }
        if(extraDirConfig.get().dirs.isNotEmpty()) {
            cs.with(CopySpecUtil.getCSForServerDir(project, extraDirConfig.get()))
        }

        cs.includeEmptyDirs = true

        fsOps.copy {
            it.with(cs)
            it.into(outputDirectory.get())
        }
    }
}
