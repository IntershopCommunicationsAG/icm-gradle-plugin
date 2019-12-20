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
package com.intershop.gradle.icm.extension

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty

/**
 * Extension needed for the configuration of an INTERSHOP base project.
 */
open class BaseConfiguration(project: Project) {

    private val devCartridgeListProperty: ListProperty<String> = project.objects.listProperty(String::class.java)
    private val testCartridgeListProperty: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * List of development cartridges in project.
     *
     * @property devCartridgeList list of identified development cartridges
     */
    val devCartridgeList: List<String>
        get() = devCartridgeListProperty.get()

    /**
     * Add an dev cartridge to the list.
     *
     * @param projectName name of the dev cartridge
     */
    fun addDevCartridge(projectName: String) {
        devCartridgeListProperty.add(projectName)
    }

    /**
     * List of test cartridges in project.
     *
     * @property testCartridgeList list of identified test cartridges
     */
    val testCartridgeList: List<String>
        get() = testCartridgeListProperty.get()

    /**
     * Add an test cartridge to the list.
     *
     * @param projectName name of the test cartridge
     */
    fun addTestCartridge(projectName: String) {
        testCartridgeListProperty.add(projectName)
    }
}
