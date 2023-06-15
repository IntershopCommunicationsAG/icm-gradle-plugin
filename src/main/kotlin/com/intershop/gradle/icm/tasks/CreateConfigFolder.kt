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

import com.intershop.gradle.icm.project.TargetConf
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Task for folder creation of configuration files of a server.
 *
 * @property projectLayout service object for project layout handling
 * @property objectFactory service object for object handling
 * @property fsOps service object for file system operations
 * @constructor Creates a task for folder handling.
 */
open class CreateConfigFolder
        @Inject constructor(
            projectLayout: ProjectLayout,
            objectFactory: ObjectFactory,
            fsOps: FileSystemOperations): AbstractCreateFolder(projectLayout, objectFactory, fsOps) {

    init {
        outputDir.convention(TargetConf.DEVELOPMENT.config(projectLayout))
    }

    @get:InputFile
    val versionInfo: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Provides the version information of this project. This file is used for
     * the presentation in the backoffice login.
     *
     * @param file regular file provider.
     */
    fun provideVersionInfoFile(file: Provider<RegularFile>) = versionInfo.set(file)

    override fun addPackages(cs: CopySpec) {
        val fileCS = project.copySpec()
        fileCS.from(versionInfo.get())
        fileCS.into("system-conf/cluster")

        cs.with(fileCS)
    }

    /**
     * Task method of this task. It creates
     * the configuration folder structure for the server.
     */
    @TaskAction
    fun executeTask() {
        createFolder()
    }
}
