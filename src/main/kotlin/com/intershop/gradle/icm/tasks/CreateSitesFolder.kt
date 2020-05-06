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

import com.intershop.gradle.icm.ICMProjectPlugin.Companion.SITES_FOLDER
import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Task for folder creation of sites files of a server.
 *
 * @property projectLayout service object for project layout handling
 * @property objectFactory service object for object handling
 * @property fsOps service object for file system operations
 * @constructor Creates a task for folder handling.
 */
open class CreateSitesFolder
        @Inject constructor(projectLayout: ProjectLayout,
                            objectFactory: ObjectFactory,
                            fsOps: FileSystemOperations): AbstractCreateFolder(projectLayout, objectFactory, fsOps) {

    init {
        outputDir.convention(projectLayout.buildDirectory.dir("$SITES_FOLDER/sites"))
    }

    override fun addPackages(cs: CopySpec) {
        PackageUtil.addPackageToCS(
            project = project,
            dependency = baseProject.get().dependency.get(),
            classifier = "sites",
            copySpec = cs,
            filePackage = baseProject.get().sitesPackage,
            excludes = listOf())
        modules.get().forEach { prj ->
            PackageUtil.addPackageToCS(
                project = project,
                dependency = prj.dependency.get(),
                classifier = "sites",
                copySpec = cs,
                filePackage = prj.sitesPackage,
                excludes = listOf())
        }
    }

    /**
     * Task method of this task. It creates
     * the sites structure for the server.
     */
    @TaskAction
    fun createSites() {
        createFolder()
    }
}
