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

import com.intershop.gradle.icm.ICMProductPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import java.io.File

/**
 * CreateServerDirProperties Gradle task 'createServerDirProperties'
 *
 * This task creates a configuration file with all available project
 * directories of the ICM server.
 */
class CreateServerDirProperties : DefaultTask() {

    private val outputFileProperty: RegularFileProperty = project.objects.fileProperty()

    /**
     * The output file contains the properties file with the complete directory configuration of ICM.
     *
     * @property outputFile real file on file system with descriptor
     */
    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)

    init {
        outputFile = File(
            project.buildDir,
            "${ICMProductPlugin.SERVER_DIRECTORY_PROPERTIES_DIR}/${ICMProductPlugin.SERVER_DIRECTORY_PROPERTIES}"
        )
    }


}
