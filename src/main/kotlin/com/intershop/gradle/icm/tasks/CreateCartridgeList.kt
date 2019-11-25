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

import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * This task creates a cartridge list properties
 * file from a template file.
 */
open class CreateCartridgeList: DefaultTask() {

    private val outputFileProperty: RegularFileProperty = project.objects.fileProperty()
    private val templateFileProperty: RegularFileProperty = project.objects.fileProperty()

    private val includesListProperty: ListProperty<String> = project.objects.listProperty(String::class.java)
    private val excludesListProperty: ListProperty<String> = project.objects.listProperty(String::class.java)

    companion object {
        const val DEFAULT_NAME = "createCartridgeList"
    }

    init {
        group = "intershop"
        description = "Creates a cartridge list properties file from template."

        outputFileProperty.set(File(project.buildDir, "cartridgelist/cartridgelist.properties" ))
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

    /**
     * Input template file with all cartridges.
     *
     * @property tempateFile
     */
    @get:InputFile
    var templateFile: File
        get() = templateFileProperty.get().asFile
        set(value) = templateFileProperty.set(value)

    /**
     * Set provider for includes matches.
     *
     * @param includes list of includes matches.
     */
    @Suppress("unused")
    fun includes(includes: Provider<List<String>>) =
        includesListProperty.set(includes)

    /**
     * This list contains includes for cartridge list.
     *
     * @property includes list of includes
     */
    @get:Input
    var includes by includesListProperty

    /**
     * Add includes matches to the list of includes.
     *
     * @param includes includes matches
     */
    fun include(vararg includes: Any) {
        includes.forEach {
            includesListProperty.add(it.toString())
        }
    }

    /**
     * Set provider for excludes matches.
     *
     * @param excludes list of includes matches.
     */
    @Suppress("unused")
    fun excludes(includes: Provider<List<String>>) =
        excludesListProperty.set(includes)

    /**
     * This list contains excludes for cartridge list.
     *
     * @property excludes list of includes
     */
    @get:Input
    var excludes by excludesListProperty

    /**
     * Add excludes matches to the list of excludes.
     *
     * @param includes includes matches
     */
    fun exclude(vararg excludes: Any) {
        excludes.forEach {
            excludesListProperty.add(it.toString())
        }
    }

    /**
     * This function represents the logic of this task.
     */
    @TaskAction
    fun createCartridgeList() {
        val baseFile = templateFileProperty.get().asFile
        if(! baseFile.exists()) {
            throw GradleException("File '" + baseFile.absolutePath + "' does not exists.")
        }

        val file = outputFileProperty.get().asFile

        baseFile.forEachLine lineIt@{ tline ->
            var line = tline.replace("\\", "").trim()
            excludesListProperty.get().forEach exIt@{ exRegex ->
                if(line.matches(exRegex.toRegex())) {
                    line = ""
                    return@exIt
                }
            }
            if( line != "" && includesListProperty.get().size > 0 ) {
                includesListProperty.get().forEach inIt@{ inRegex ->
                    if(line.matches(inRegex.toRegex())) {
                        return@inIt
                    }
                }
            }

            if(line != "") {
                file.appendText(tline + "\n")
            }
        }
    }
}
