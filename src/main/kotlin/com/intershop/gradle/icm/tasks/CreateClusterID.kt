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
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.*
import javax.inject.Inject

/**
 * Gradle task to create an ID for ICM cluster.
 *
 * This taks creates an UID with Java functionality
 * in the required format.
 */
open class CreateClusterID @Inject constructor(
        projectLayout: ProjectLayout,
        objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "createClusterID"
        const val CLUSTER_ID_NAME = "cluster.id"
    }

    /**
     * Output file for generated cluster id.
     *
     * @property outputDir
     */
    @get:OutputDirectory
    val outputDir: DirectoryProperty = objectFactory.directoryProperty()

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Creates a cluster ID to start a server."

        outputDir.convention(projectLayout.buildDirectory.dir("clusterIDDir"))
    }

    /**
     * This function represents the logic of this task.
     */
    @TaskAction
    fun createID() {
        val uuid = UUID.randomUUID()
        val uuidStr = uuid.toString().replace("-", "")
        val outputFile = outputDir.get().file(CLUSTER_ID_NAME).asFile

        if(! outputFile.parentFile.exists()) {
            project.delete(outputFile.parentFile)
            outputFile.parentFile.mkdirs()
        }

        outputFile.writeText(uuidStr)
    }

}
