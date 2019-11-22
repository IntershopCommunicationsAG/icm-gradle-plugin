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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*

open class CreateClusterID: DefaultTask() {

    private val outputFileProperty: RegularFileProperty = project.objects.fileProperty()

    companion object {
        const val DEFAULT_NAME = "createClusterID"
    }

    init {
        group = "intershop"
        description = "Creates a cluster ID to start a server."

        outputFileProperty.set(File(project.buildDir, "clusterIDDir/cluster.id" ))
    }

    /**
     * Output file for generated cluster id.
     *
     * @property outputFile
     */
    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)

    @TaskAction
    fun createID() {
        val uuid = UUID.randomUUID()
        val uuidStr = uuid.toString().replace("-", "")
        val outputFile = outputFileProperty.get().asFile

        if(! outputFile.getParentFile().exists()) {
            project.delete(outputFile.getParentFile())
            outputFile.getParentFile().mkdirs()
        }

        outputFile.writeText(uuidStr)
    }

}