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

/**
 * Extension of an Intershop ICM project.
 */
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
    /**
     * Inject service of ProjectLayout (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val projectLayout: ProjectLayout

    private val cartridgeDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    init {
        cartridgeDirProperty.convention(projectLayout.buildDirectory.dir(EXTERNAL_CARTRIDGE_PATH))
    }

    /**
     * Provider of cartridge directory for external cartridges.
     *
     * @property cartridgeDirProvider
     */
    val cartridgeDirProvider: Provider<Directory>
        get() = cartridgeDirProperty

    /**
     * Cartridge directory for external cartridges.
     *
     * @property cartridgeDir
     */
    var cartridgeDir: File
        get() = cartridgeDirProperty.get().asFile
        set(value) = cartridgeDirProperty.set(value)

    /**
     * Set only a short path for cartridge dir.
     * @param buildPath
     */
    fun setCartridgeBuildPath(buildPath: String) {
        cartridgeDirProperty.set(projectLayout.buildDirectory.dir(buildPath))
    }
}
