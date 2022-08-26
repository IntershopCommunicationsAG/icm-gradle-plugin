/*
 * Copyright 2021 Intershop Communications AG.
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
package com.intershop.gradle.icm.tasks.crossproject

import com.intershop.gradle.icm.ICMProjectPlugin
import com.intershop.gradle.icm.tasks.CreateConfigFolder
import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import java.io.File
import javax.inject.Inject

open class PrepareConfigFolder
    @Inject constructor(
        projectLayout: ProjectLayout,
        objectFactory: ObjectFactory,
        fsOps: FileSystemOperations): CreateConfigFolder(projectLayout, objectFactory, fsOps) {

    init {
        outputDir.set(File(project.buildDir, "compositeserver/conf"))
    }

    fun provideMainBaseDir(baseDir: Provider<Directory>) = mainBaseDir.set(baseDir)

    @get:InputDirectory
    val mainBaseDir: DirectoryProperty = objectFactory.directoryProperty()

    fun provideModuleDirectories(moduleDirMap: Provider<MutableMap<String, File>>) = moduleDirectories.set(moduleDirMap)

    @get:Input
    val moduleDirectories: MapProperty<String, File> = objectFactory.mapProperty(String::class.java, File::class.java)

    override fun addPackages(cs: CopySpec) {
        PackageUtil.addPackageToCS(
            project = project,
            dependency = baseProject.get().dependency.get(),
            classifier = "configuration",
            copySpec = cs,
            filePackage = baseProject.get().configPackage,
            excludes = listOf("**/cluster/${ICMProjectPlugin.CARTRIDGELIST_FILENAME}",
                "**/cluster/${CreateServerInfo.VERSIONINFO_FILENAME}"),
            fileBase = mainBaseDir.get().asFile)

        modules.get().forEach { prj ->
            val dep = prj.dependency.get()
            var key = ""
            if(dep.isNotBlank()) {
                val depList = dep.split(":")
                key = if(depList.size > 1) { "${depList[0]}_${depList[1]}" } else { "" }
            }
            val dir = if(key.isNotBlank()) { moduleDirectories.get()[key] } else { null }

            PackageUtil.addPackageToCS(
                project = project,
                dependency = prj.dependency.get(),
                classifier = "configuration",
                copySpec = cs,
                filePackage = prj.configPackage,
                excludes = listOf("**/cluster/${ICMProjectPlugin.CARTRIDGELIST_FILENAME}",
                    "**/cluster/${CreateServerInfo.VERSIONINFO_FILENAME}"),
                fileBase = dir)
        }

        val fileCS = project.copySpec()
        fileCS.from(versionInfo.get())
        fileCS.into("system-conf/cluster")

        cs.with(fileCS)
    }
}
