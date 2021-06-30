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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class CreateSitesFolder @Inject constructor(
    @Internal val projectLayout: ProjectLayout,
    objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "createSitesFolder"
        const val DEFAULT_DIR_NAME = "sites_folder"
    }

    @get:Input
    val foldername: Property<String> = objectFactory.property(String::class.java)

    /**
     * Output file for generated cluster id.
     *
     * @property outputFile
     */
    @get:OutputDirectory
    val outputDir: DirectoryProperty = objectFactory.directoryProperty()

    init {
        group = "ICM server build"
        description = "Creates the folder for the local sites folder"
        foldername.convention(DEFAULT_DIR_NAME)
        outputDir.convention(projectLayout.buildDirectory.dir(foldername.get()))
    }

    @TaskAction
    fun createsFolder() {
        val sd = outputDir.get().asFile
        if(sd.exists() && sd.isFile) {
            throw GradleException("There is file '" + sd.absolutePath + "'. The directory is not created.")
        }
        if(! sd.exists()) {
            sd.mkdirs()
        }
    }
}
