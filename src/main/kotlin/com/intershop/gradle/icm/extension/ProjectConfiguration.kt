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
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.util.ConfigureUtil
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
        const val EXTERNAL_CARTRIDGE_PATH = "server/cartridges"

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

    init {
        cartridgeDirProperty.convention(projectLayout.buildDirectory.dir(EXTERNAL_CARTRIDGE_PATH))
    }

    /**
     * Base project configuration for final project.
     *
     * @property baseProjects
     */
    val baseProjects: NamedDomainObjectContainer<BaseProjectConfiguration>
            = objectFactory.domainObjectContainer(BaseProjectConfiguration::class.java)

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
     * @param cartridge name of an ICM cartridge
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
     * @param cartridge name of an ICM cartridge
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
     * @param cartridge name of an ICM cartridge
     */
    fun productionCartridge(cartridge: String) {
        productionCartridgesProperty.add(cartridge)
    }

    /**
     * Configuration for configuration folder.
     *
     * @property conf
     */
    val conf : DirConf = objectFactory.newInstance(DirConf::class.java)

    /**
     * Configures conf with an action.
     *
     * @param action
     */
    fun conf(action: Action<in DirConf>) {
        action.execute(conf)
    }

    /**
     * Configures conf with a closure.
     *
     * @param c
     */
    fun conf(c: Closure<DirConf>) {
        ConfigureUtil.configure(c, conf)
    }

    /**
     * Configuration for sites folder.
     *
     * @property sites
     */
    val sites: DirConf = objectFactory.newInstance(DirConf::class.java)

    /**
     * Configures sitesPackage with an action.
     *
     * @param action
     */
    fun sites(action: Action<in DirConf>) {
        action.execute(sites)
    }

    /**
     * Configures sites with a closure.
     *
     * @param c
     */
    fun sites(c: Closure<DirConf>) {
        ConfigureUtil.configure(c, sites)
    }

    /**
     * Configuration for developer / test configuration folder.
     *
     * @property devConf
     */
    val devConf : DirConf = objectFactory.newInstance(DirConf::class.java)

    /**
     * Configures conf with an action.
     *
     * @param action
     */
    fun devConf(action: Action<in DirConf>) {
        action.execute(conf)
    }

    /**
     * Configures conf with a closure.
     *
     * @param c
     */
    fun devConf(c: Closure<DirConf>) {
        ConfigureUtil.configure(c, devConf)
    }

    /**
     * Configuration for developer / test sites folder.
     *
     * @property devSites
     */
    val devSites: DirConf = objectFactory.newInstance(DirConf::class.java)

    /**
     * Configures sitesPackage with an action.
     *
     * @param action
     */
    fun devSites(action: Action<in DirConf>) {
        action.execute(devSites)
    }

    /**
     * Configures sites with a closure.
     *
     * @param c
     */
    fun devSites(c: Closure<DirConf>) {
        ConfigureUtil.configure(c, devSites)
    }

}
