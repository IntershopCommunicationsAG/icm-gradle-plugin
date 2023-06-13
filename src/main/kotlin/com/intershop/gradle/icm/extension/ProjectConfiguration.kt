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

import com.intershop.gradle.icm.project.TargetConf
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject

/**
 * Extension of an Intershop ICM project.
 *
 * @constructor creates the extension of a project configuration.
 */
open class ProjectConfiguration
    @Inject constructor(
        val project: Project,
        objectFactory: ObjectFactory,
        projectLayout: ProjectLayout) {

    val containerConfig: File = TargetConf.PRODUCTION.config(projectLayout).get().asFile
    val testcontainerConfig: File = TargetConf.TEST.config(projectLayout).get().asFile

    val config: File = TargetConf.DEVELOPMENT.config(projectLayout).get().asFile
    @Deprecated("newBaseProject feature is unsupported since 5.6.0", level = DeprecationLevel.WARNING)
    val newBaseProject: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Base project configuration for final project.
     *
     * @property base
     */
    val base: CartridgeProject = objectFactory.newInstance(CartridgeProject::class.java, project)

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
     * @param closure Closure to configure Cartridge project (ICM)
     */
    fun base(closure: Closure<CartridgeProject>) {
        project.configure(base, closure)
    }

    val modules: NamedDomainObjectContainer<NamedCartridgeProject> =
        objectFactory.domainObjectContainer(
            NamedCartridgeProject::class.java,
            NamedCartridgeProjectFactory(project, objectFactory))

    val libFilterFileDependency: Property<String> = objectFactory.property(String::class.java)

    @Deprecated("Configuration via folder is unsupported since 5.6.0", level = DeprecationLevel.WARNING)
    val serverDirConfig: ProjectServerDirs = objectFactory.newInstance(ProjectServerDirs::class.java, project)

    /**
     * Configures the directory configuration of the project.
     *
     * @param action Action to configure project server dirs
     */
    @Deprecated("Configuration via folder is unsupported since 5.6.0", level = DeprecationLevel.WARNING)
    fun serverDirConfig(action: Action<in ProjectServerDirs>) {
        action.execute(serverDirConfig)
    }

    /**
     * Configures the directory configuration of the project.
     *
     * @param closure Closure to configure project server dirs
     */
    @Deprecated("Configuration via folder is unsupported since 5.6.0", level = DeprecationLevel.WARNING)
    fun serverDirConfig(closure: Closure<ProjectServerDirs>) {
        project.configure(serverDirConfig, closure)
    }

}

