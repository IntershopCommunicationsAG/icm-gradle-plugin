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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.CopySpec
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

    val confCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)
    val sitesCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)

    val devConfCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)
    val devSitesCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)

    init {
        cartridgeDirProperty.convention(projectLayout.buildDirectory.dir(EXTERNAL_CARTRIDGE_PATH))
    }

    /**
     * Base project configuration for final project
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
     * Provider of project configuration directory.
     *
     * @property confCopySpecProvider
     */
    val confCopySpecProvider: Provider<CopySpec>
        get() = confCopySpecProperty

    /**
     * Additional project configuration directory.
     *
     * @property confCopySpec
     */
    var confCopySpec: CopySpec
        get() = confCopySpecProperty.get()
        set(value) = confCopySpecProperty.set(value)

    /**
     * Provider of project sites directory.
     *
     * @property sitesCopySpecProperty
     */
    val sitesCopySpecProvider: Provider<CopySpec>
        get() = sitesCopySpecProperty

    /**
     * Project sites directory.
     *
     * @property sitesCopySpec
     */
    var sitesCopySpec: CopySpec
        get() = sitesCopySpecProperty.get()
        set(value) = sitesCopySpecProperty.set(value)

    /* Provider of project configuration directory.
    *
    * @property devConfCopySpecProvider
    */
    val devConfCopySpecProvider: Provider<CopySpec>
        get() = devConfCopySpecProperty

    /**
     * Additional project configuration directory.
     *
     * @property confCopySpec
     */
    var devConfCopySpec: CopySpec
        get() = devConfCopySpecProperty.get()
        set(value) = devConfCopySpecProperty.set(value)

    /**
     * Provider of project sites directory.
     *
     * @property sitesCopySpecProperty
     */
    val devSitesCopySpecProvider: Provider<CopySpec>
        get() = devSitesCopySpecProperty

    /**
     * Project sites directory.
     *
     * @property sitesCopySpec
     */
    var devSitesCopySpec: CopySpec
        get() = devSitesCopySpecProperty.get()
        set(value) = devSitesCopySpecProperty.set(value)

}
