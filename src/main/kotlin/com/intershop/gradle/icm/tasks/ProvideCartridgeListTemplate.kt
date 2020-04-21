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

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.ExtendCartridgeList.Companion.CARTRIDGELISTFILE_NAME
import com.intershop.gradle.icm.utils.PackageUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class ProvideCartridgeListTemplate @Inject constructor(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory,
    private var fsOps: FileSystemOperations) : DefaultTask() {

    @get:Optional
    @get:Input
    val baseDependency: Property<String> = objectFactory.property(String::class.java)

    fun provideBaseDependency(dependency: Provider<String>) = baseDependency.set(dependency)

    @get:Optional
    @get:Input
    val fileDependency: Property<String> = objectFactory.property(String::class.java)

    fun provideFileDependency(dependency: Provider<String>) = fileDependency.set(dependency)

    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    fun provideOutputFile(file: Provider<RegularFile>) = outputFile.set(file)

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Download a file cartridgelist.properties for further steps."

        outputFile.convention(projectLayout.buildDirectory.file("cartridgelisttemplate/cartridgelist.properties"))
    }

    @TaskAction
    fun downloadFile() {
       if(fileDependency.isPresent && fileDependency.get().isNotEmpty()) {
            fileDependency.get()

        } else {
            baseDependency.get()
            val file = PackageUtil.downloadPackage(project, baseDependency.get(), "configuration")
            if(file != null) {
                val pfiles = project.zipTree(file).matching { pf ->
                    pf.include("**/**/${CARTRIDGELISTFILE_NAME}")
                }
                if (!pfiles.isEmpty) {
                    fsOps.copy {
                        it.from(pfiles.asPath)
                        it.into(outputFile.get().asFile.parent)
                    }
                }
            } else {
                throw GradleException("Configuration package is not available in the configured base project (${baseDependency.get()})")
            }
        }
    }
}