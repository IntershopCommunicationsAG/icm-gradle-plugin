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

import com.intershop.gradle.icm.ICMProjectPlugin.Companion.CONFIG_FOLDER
import com.intershop.gradle.icm.ICMProjectPlugin.Companion.PROD_CONTAINER_FOLDER
import com.intershop.gradle.icm.ICMProjectPlugin.Companion.SERVER_FOLDER
import com.intershop.gradle.icm.ICMProjectPlugin.Companion.TEST_CONTAINER_FOLDER
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.util.ConfigureUtil
import java.io.File
import javax.inject.Inject

/**
 * Extension of an Intershop ICM project.
 */
abstract class ProjectConfiguration @Inject constructor(objectFactory: ObjectFactory, projectLayout: ProjectLayout) {

    private val cartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val dbprepareCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)


    val prodConfigFolder: File = projectLayout.buildDirectory.dir("$PROD_CONTAINER_FOLDER/$CONFIG_FOLDER").get().asFile

    val testConfigFolder: File = projectLayout.buildDirectory.dir("$TEST_CONTAINER_FOLDER/$CONFIG_FOLDER").get().asFile

    val developmentConfigFolder: File = projectLayout.buildDirectory.dir("$SERVER_FOLDER/$CONFIG_FOLDER").get().asFile

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
     * @property modules
     */
    val modules: NamedDomainObjectContainer<NamedCartridgeProject> = objectFactory.domainObjectContainer(NamedCartridgeProject::class.java)

    val cartridgeListDependencyProvider: Provider<String>
        get() = cartridgeListDependency

    var cartridgeListDependency: Property<String> = objectFactory.property(String::class.java)

    val libFilterFileDependencyProvider: Provider<String>
        get() = libFilterFileDependency

    var libFilterFileDependency: Property<String> = objectFactory.property(String::class.java)

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

    val serverDirConfig: ProjectServerDirs = objectFactory.newInstance(ProjectServerDirs::class.java)

    fun serverDirConfig(action: Action<in ProjectServerDirs>) {
        action.execute(serverDirConfig)
    }

    fun serverDirConfig(c: Closure<ProjectServerDirs>) {
        ConfigureUtil.configure(c, serverDirConfig)
    }
}
