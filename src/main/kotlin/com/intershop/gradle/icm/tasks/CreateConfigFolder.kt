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

import com.intershop.gradle.icm.ICMProjectPlugin.Companion.CARTRIDGELIST_FILENAME
import com.intershop.gradle.icm.tasks.CreateServerInfo.Companion.VERSIONINFO_FILENAME
import com.intershop.gradle.icm.utils.PackageUtil
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

open class CreateConfigFolder
        @Inject constructor(
            projectLayout: ProjectLayout,
            objectFactory: ObjectFactory,
            fsOps: FileSystemOperations): AbstractCreateFolder(projectLayout, objectFactory, fsOps) {

    init {
        outputDirProperty.convention(projectLayout.buildDirectory.dir("server/system-conf"))
    }

    @get:InputFile
    val cartridgeList: RegularFileProperty = objectFactory.fileProperty()

    fun provideCartridgeListFile(file: Provider<RegularFile>) = cartridgeList.set(file)

    @get:InputFile
    val versionInfo: RegularFileProperty = objectFactory.fileProperty()

    fun provideVersionInfoFile(file: Provider<RegularFile>) = versionInfo.set(file)

    override fun addPackages(cs: CopySpec) {
        PackageUtil.addPackageToCS(
            project = project,
            dependency = baseProject.get().dependency.get(),
            classifier = "configuration",
            copySpec = cs,
            filePackage = baseProject.get().configPackage,
            excludes = listOf("**/cluster/${CARTRIDGELIST_FILENAME}", "**/cluster/${VERSIONINFO_FILENAME}"))
        modules.get().forEach { (_, prj) ->
            PackageUtil.addPackageToCS(
                project = project,
                dependency = prj.dependency.get(),
                classifier = "configuration",
                copySpec = cs,
                filePackage = prj.configPackage,
                excludes = listOf("**/cluster/${CARTRIDGELIST_FILENAME}", "**/cluster/${VERSIONINFO_FILENAME}"))
        }

        val fileCS = project.copySpec()
        fileCS.from(cartridgeList.get())
        fileCS.from(versionInfo.get())
        fileCS.into("system-conf/cluster")

        cs.with(fileCS)
    }

    @TaskAction
    fun createSites() {
        createFolder()
    }
}