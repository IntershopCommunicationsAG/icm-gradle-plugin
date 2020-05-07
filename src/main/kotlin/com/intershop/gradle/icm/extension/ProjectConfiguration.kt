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
import com.intershop.gradle.icm.ICMProjectPlugin.Companion.SITES_FOLDER
import com.intershop.gradle.icm.ICMProjectPlugin.Companion.TEST_CONTAINER_FOLDER
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.util.ConfigureUtil
import java.io.File
import javax.inject.Inject

/**
 * Extension of an Intershop ICM project.
 *
 * @constructor creates the extension of a project configuration.
 */
abstract class ProjectConfiguration @Inject constructor(objectFactory: ObjectFactory, projectLayout: ProjectLayout) {

    val containerConfig: File = projectLayout.buildDirectory.dir(
        "$PROD_CONTAINER_FOLDER/$CONFIG_FOLDER").get().asFile

    val testcontainerConfig: File = projectLayout.buildDirectory.dir(
        "$TEST_CONTAINER_FOLDER/$CONFIG_FOLDER").get().asFile

    val config: File = projectLayout.buildDirectory.dir(
        "$SERVER_FOLDER/$CONFIG_FOLDER").get().asFile

    val containerSites: File = projectLayout.buildDirectory.dir(
        "$PROD_CONTAINER_FOLDER/$SITES_FOLDER").get().asFile

    val testcontainerSites: File = projectLayout.buildDirectory.dir(
        "$TEST_CONTAINER_FOLDER/$SITES_FOLDER").get().asFile

    val sites: File = projectLayout.buildDirectory.dir(
        "$SERVER_FOLDER/$SITES_FOLDER").get().asFile

    val newBaseProject: Property<Boolean> = objectFactory.property(Boolean::class.java)

    /**
     * Base project configuration for final project.
     *
     * @property base
     */
    val base: CartridgeProject = objectFactory.newInstance(CartridgeProject::class.java)

    /**
     * Configures a binary base project (This is also connected to
     * a Docker image.).
     *
     * @param action Action to configure Cartridge project (ICM)
     */
    fun base(action: Action<in CartridgeProject>) {
        action.execute(base)
    }

    /**
     * Configures a binary base project (This is also connected to
     * a Docker image.).
     *
     * @param c Closure to configure Cartridge project (ICM)
     */
    fun base(c: Closure<CartridgeProject>) {
        ConfigureUtil.configure(c, base)
    }

    val modules: NamedDomainObjectContainer<NamedCartridgeProject> =
        objectFactory.domainObjectContainer(NamedCartridgeProject::class.java)

    val cartridgeListDependency: Property<String> = objectFactory.property(String::class.java)

    val libFilterFileDependency: Property<String> = objectFactory.property(String::class.java)

    /**
     * Cartridges of this project.
     *
     * @property cartridges
     */
    val cartridges: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Add a single cartridge to the list.
     *
     * @param cartridge name of an ICM cartridge
     */
    fun cartridge(cartridge: String) {
        cartridges.add(cartridge)
    }

    /**
     * Cartridges of this project.
     *
     * @property dbprepareCartridges
     */
    var dbprepareCartridges: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Add a single cartridge to the list of dbprepare cartridges.
     *
     * @param cartridge name of an ICM cartridge
     */
    fun dbprepareCartridge(cartridge: String) {
        dbprepareCartridges.add(cartridge)
    }

    val serverDirConfig: ProjectServerDirs = objectFactory.newInstance(ProjectServerDirs::class.java)

    /**
     * Configures the directory configuration of the project.
     *
     * @param action Action to configure project server dirs
     */
    fun serverDirConfig(action: Action<in ProjectServerDirs>) {
        action.execute(serverDirConfig)
    }

    /**
     * Configures the directory configuration of the project.
     *
     * @param c Closure to configure project server dirs
     */
    fun serverDirConfig(c: Closure<ProjectServerDirs>) {
        ConfigureUtil.configure(c, serverDirConfig)
    }

    init {
        newBaseProject.set(false)
    }
}

