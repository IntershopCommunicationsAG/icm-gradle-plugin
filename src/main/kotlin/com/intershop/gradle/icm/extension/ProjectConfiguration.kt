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

import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import java.io.File
import javax.inject.Inject

/**
 * Extension of an Intershop ICM project.
 */
abstract class ProjectConfiguration {

    companion object {
        /**
         * Path in build directory for external cartridges.
         */
        const val EXTERNAL_CARTRIDGE_PATH = "ext_cartridges"

        /**
         * Path for original cartridge list properties in build directory.
         */
        const val CARTRIDGELISTFILE_PATH = "org_cartridgelist/cartridgelist.properties"
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

    private val cartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val dbprepareCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val productionCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)

    private val configurationPackageProperty: Property<String> = objectFactory.property(String::class.java)
    private val sitesPackageProperty: Property<String> = objectFactory.property(String::class.java)

    private val configDirProperty: DirectoryProperty = objectFactory.directoryProperty()
    private val sitesDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    init {
        cartridgeDirProperty.convention(projectLayout.buildDirectory.dir(EXTERNAL_CARTRIDGE_PATH))
        configDirProperty.convention(projectLayout.projectDirectory.dir("config/base"))
        sitesDirProperty.convention(projectLayout.projectDirectory.dir("sites/base"))
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

    /**
     * Provider of cartridge list extension of a project.
     *
     * @property cartridgesProperty
     */
    val cartridgesProvider: Provider<Set<String>>
        get() = cartridgesProperty

    /**
     * Cartridges of this project.
     *
     * @property cartridges
     */
    var cartridges by cartridgesProperty

    /**
     * Add a single cartridge to the list.
     *
     * @param cartrige name of an ICM cartridge
     */
    fun cartridge(cartridge: String) {
        cartridgesProperty.add(cartridge)
    }

    /**
     * Provider of dbinit/dbprepare cartridge list extension of a project.
     *
     * @property dbprepareCartridgesProperty
     */
    val dbprepareCartridgesProvider: Provider<Set<String>>
        get() = dbprepareCartridgesProperty

    /**
     * Cartridges of this project.
     *
     * @property dbprepareCartridges
     */
    var dbprepareCartridges by dbprepareCartridgesProperty

    /**
     * Add a single cartridge to the list of dbprepare cartridges.
     *
     * @param cartrige name of an ICM cartridge
     */
    fun dbprepareCartridge(cartridge: String) {
        cartridgesProperty.add(cartridge)
    }

    /**
     * Provider of all production cartridges in the extended list.
     *
     * @property productionCartridgesProvider
     */
    val productionCartridgesProvider: Provider<Set<String>>
        get() = productionCartridgesProperty

    /**
     * Cartridges of this project.
     *
     * @property productionCartridges
     */
    var productionCartridges by productionCartridgesProperty

    /**
     * Add a single cartridge to the list of dbprepare cartridges.
     *
     * @param cartrige name of an ICM cartridge
     */
    fun productionCartridge(cartridge: String) {
        productionCartridgesProperty.add(cartridge)
    }

    /**
     * Provider of configurationPackage property.
     *
     * @property configurationPackageProvider
     */
    val configurationPackageProvider: Provider<String>
        get() = configurationPackageProperty

    /**
     * Base of the sites configuration of an ICM server.
     *
     * @property configurationPackage
     */
    var configurationPackage by configurationPackageProperty

    /**
     * Provider of configurationPackage property.
     *
     * @property configurationPackageProvider
     */
    val sitesPackageProvider: Provider<String>
        get() = sitesPackageProperty

    /**
     * Base of the server configuration of an ICM server.
     *
     * @property sitesPackage
     */
    var sitesPackage by sitesPackageProperty

    /**
     * Provider of project configuration directory.
     *
     * @property configDirProvider
     */
    val configDirProvider: Provider<Directory>
        get() = configDirProperty

    /**
     * Additional project configuration directory.
     *
     * @property configDir
     */
    var configDir: File
        get() = configDirProperty.get().asFile
        set(value) = configDirProperty.set(value)

    /**
     * Set only a short path for project configuration dir.
     * @param configPath
     */
    fun setConfigPath(configPath: String) {
        configDirProperty.set(projectLayout.projectDirectory.dir(configPath))
    }

    /**
     * Provider of project sites directory.
     *
     * @property sitesDirProvider
     */
    val sitesDirProvider: Provider<Directory>
        get() = sitesDirProperty

    /**
     * Project sites directory.
     *
     * @property sitesDir
     */
    var sitesDir: File
        get() = sitesDirProperty.get().asFile
        set(value) = sitesDirProperty.set(value)

    /**
     * Set only a short path for project configuration dir.
     * @param configPath
     */
    fun setSitesPath(sitesPath: String) {
        sitesDirProperty.set(projectLayout.projectDirectory.dir(sitesPath))
    }
}
