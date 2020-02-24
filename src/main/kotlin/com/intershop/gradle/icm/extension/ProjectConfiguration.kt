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

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

abstract class ProjectConfiguration {

    companion object {
        // names for the plugin
        const val EXTERNAL_CARTRIDGE_PATH = "ext_cartridges"
    }

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    @get:Inject
    abstract val projectLayout: ProjectLayout

    private val cartridgeDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    init {
        cartridgeDirProperty.convention(projectLayout.buildDirectory.dir(EXTERNAL_CARTRIDGE_PATH))
    }

    val cartridgeDirProvider: Provider<Directory>
        get() = cartridgeDirProperty

    var cartridgeDir: File
        get() = cartridgeDirProperty.get().asFile
        set(value) = cartridgeDirProperty.set(value)

    fun setCartridgeBuildPath(buildPath: String) {
        cartridgeDirProperty.set(projectLayout.buildDirectory.dir(buildPath))
    }
}