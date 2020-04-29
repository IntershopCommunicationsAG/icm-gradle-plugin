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
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * This task creates a cartridge list properties
 * file from a template file.
 */
open class CreateCartridgeList @Inject constructor(
        projectLayout: ProjectLayout,
        objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "createCartridgeList"
        const val CARTRIDGE_LIST = "cartridgelist/cartridgelist.properties"
    }

    /**
     * Output file for generated cluster id.
     *
     * @property outputFile
     */
    @get:OutputFile
    var outputFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Input template file with all cartridges.
     *
     * @property templateFile
     */
    @get:InputFile
    var templateFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Set provider for includes matches.
     *
     * @param provider list of includes matches.
     */
    @Suppress("unused")
    fun includes(provider: Provider<List<String>>) = includes.set(provider)

    /**
     * This list contains includes for cartridge list.
     *
     * @property includes list of includes
     */
    @get:Input
    var includes: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * Add includes matches to the list of includes.
     *
     * @param params includes matches
     */
    fun include(vararg params: Any) {
        params.forEach {
            includes.add(it.toString())
        }
    }

    /**
     * Set provider for excludes matches.
     *
     * @param provider list of includes matches.
     */
    @Suppress("unused")
    fun excludes(provider: Provider<List<String>>) = excludes.set(provider)

    /**
     * This list contains excludes for cartridge list.
     *
     * @property excludes list of includes
     */
    @get:Input
    var excludes: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * Add excludes matches to the list of excludes.
     *
     * @param params includes matches
     */
    fun exclude(vararg params: Any) {
        params.forEach {
            excludes.add(it.toString())
        }
    }

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Creates a cartridge list properties file from template."

        outputFile.convention(projectLayout.buildDirectory.file(CARTRIDGE_LIST))
    }

    /**
     * This function represents the logic of this task.
     */
    @TaskAction
    fun createCartridgeList() {
        val baseFile = templateFile.get().asFile
        if(! baseFile.exists()) {
            throw GradleException("File '" + baseFile.absolutePath + "' does not exists.")
        }

        val file = outputFile.get().asFile

        if(file.exists()) {
            project.delete(file)
        }

        val entries = mutableListOf<String>()

        baseFile.forEachLine lineIt@{ tline ->
            val uline = tline.replace("\\", "").trim()
            var line = uline
            excludes.get().forEach exIt@{ exRegex ->
                if(uline.matches(exRegex.toRegex())) {
                    line = ""
                    return@exIt
                }
            }
            if(line == "") {
                includes.get().forEach inIt@{ inRegex ->
                    if (uline.matches(inRegex.toRegex())) {
                        line = uline
                        return@inIt
                    }
                }
            }

            if(line != "" || uline == "") {
                if(tline == "" || tline.trim().startsWith("#")) {
                    val newLast = entries.last().replace("\\", "").trimEnd()
                    entries.removeAt(entries.lastIndex)
                    entries.add("$newLast\n")
                }
                entries.add( "$tline\n")
            }
        }

        entries.forEach {
            file.appendText(it)
        }
    }
}
