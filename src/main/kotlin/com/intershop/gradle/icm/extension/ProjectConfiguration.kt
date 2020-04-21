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
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * Extension of an Intershop ICM project.
 */
abstract class ProjectConfiguration {

    companion object {
        /**
         * Path in build directory for external cartridges.
         */
        const val EXTERNAL_CARTRIDGE_PATH = "default/cartridges"
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

    private val cartridgeListDependencyProperty: Property<String> = objectFactory.property(String::class.java)
    private val libFilterFileDependencyProperty: Property<String> = objectFactory.property(String::class.java)

    private val cartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val dbprepareCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)

    private val configFolderProperty: Property<String> = objectFactory.property(String::class.java)
    private val sitesFolderProperty: Property<String> = objectFactory.property(String::class.java)
    private val devConfigFolderProperty: Property<String> = objectFactory.property(String::class.java)
    private val devSitesFolderProperty: Property<String> = objectFactory.property(String::class.java)

    /**
     * Base project configuration for final project.
     *
     * @property base
     */
    val base: CartridgeProject = objectFactory.newInstance(CartridgeProject::class.java)

    fun base(action: Action<in CartridgeProject>) {
        action.execute(base)
    }

    fun base(c: Closure<CartridgeProject>) {
        ConfigureUtil.configure(c, base)
    }

    /**
     * Additional extension projects for final project.
     *
     * @property base
     */
    val extensions: NamedDomainObjectSet<CartridgeProject> = objectFactory.namedDomainObjectSet(CartridgeProject::class.java)

    val cartridgeListDependencyProvider: Provider<String>
        get() = cartridgeListDependencyProperty

    var cartridgeListDependency: String
        get() = cartridgeListDependencyProperty.getOrElse("")
        set(value) = cartridgeListDependencyProperty.set(value)

    val libFilterFileDependencyProvider: Provider<String>
        get() = libFilterFileDependencyProperty

    var libFilterFileDependency: String
        get() = libFilterFileDependencyProperty.getOrElse("")
        set(value) = libFilterFileDependencyProperty.set(value)

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

    val configFolderProvider: Provider<String>
        get() = configFolderProperty

    /**
     * Configuration for configuration folder.
     *
     * @property configFolder
     */
    val configFolder by configFolderProperty

    val sitesFolderProvider: Provider<String>
        get() = sitesFolderProperty

    /**
     * Configuration for sites folder.
     *
     * @property sitesFolder
     */
    val sitesFolder by sitesFolderProperty

    val devConfigFolderProvider: Provider<String>
        get() = devConfigFolderProperty

    /**
     * Configuration for developer / test configuration folder.
     *
     * @property devConfigFolder
     */
    val devConfigFolder by devConfigFolderProperty

    val devSitesFolderProvider: Provider<String>
        get() = devSitesFolderProperty

    /**
     * Configuration for developer / test sites folder.
     *
     * @property devSitesFolder
     */
    val devSitesFolder by devSitesFolderProperty

}
