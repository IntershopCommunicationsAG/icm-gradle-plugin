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

import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class CreateSitesFolder
        @Inject constructor(projectLayout: ProjectLayout,
                            objectFactory: ObjectFactory,
                            fsOps: FileSystemOperations): AbstractCreateFolder(projectLayout, objectFactory, fsOps) {

    init {
        outputDirProperty.convention(projectLayout.buildDirectory.dir("server/sites"))
    }

    override fun addPackages(cs: CopySpec) {
        PackageUtil.addPackageToCS(project, baseProject.get().dependency.get(), "sites", cs, baseProject.get().sitesPackage, listOf())
        modules.get().forEach { (_, prj) ->
            PackageUtil.addPackageToCS(project, prj.dependency.get(), "sites", cs, prj.sitesPackage, listOf())
        }
    }

    @TaskAction
    fun createSites() {
        createFolder()
    }
}