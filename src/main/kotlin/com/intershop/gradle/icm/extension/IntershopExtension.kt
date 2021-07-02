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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Extension for ICM properties.
 */
open class IntershopExtension @Inject constructor(val project: Project, objectFactory: ObjectFactory) {

    companion object {
        // names for the plugin
        const val INTERSHOP_EXTENSION_NAME = "intershop"
        const val INTERSHOP_GROUP_NAME = "Intershop Commerce Management"
    }

    val developmentConfig: DevelopmentConfiguration =
        objectFactory.newInstance(DevelopmentConfiguration::class.java)

    /**
     * Configures the development information configuration.
     *
     * @param closure closure with project information configuration
     */
    @Suppress("unused")
    fun developmentConfig(closure: Closure<DevelopmentConfiguration>) {
        project.configure(developmentConfig, closure)
    }

    /**
     * Configures the project information configuration.
     *
     * @param action action with project information configuration
     */
    fun developmentConfig(action: Action<in DevelopmentConfiguration>) {
        action.execute(developmentConfig)
    }

    val projectInfo: ProjectInfo = objectFactory.newInstance(ProjectInfo::class.java)

    /**
     * Configures the project information configuration.
     *
     * @param closure closure with project information configuration
     */
    @Suppress("unused")
    fun projectInfo(closure: Closure<ProjectInfo>) {
        project.configure(projectInfo, closure)
    }

    /**
     * Configures the project information configuration.
     *
     * @param action action with project information configuration
     */
    fun projectInfo(action: Action<in ProjectInfo>) {
        action.execute(projectInfo)
    }

    val projectConfig: ProjectConfiguration = objectFactory.newInstance(ProjectConfiguration::class.java, project)

    /**
     * Configures the base project of Intershop Commerce Management.
     *
     * @param closure closure with base project configuration of Intershop Commerce Management
     */
    @Suppress("unused")
    fun projectConfig(closure: Closure<ProjectConfiguration>) {
        project.configure(projectConfig, closure)
    }

    /**
     * Configures the base project of Intershop Commerce Management.
     *
     * @param action action with base project configuration of Intershop Commerce Management
     */
    fun projectConfig(action: Action<in ProjectConfiguration>) {
        action.execute(projectConfig)
    }

    /**
     * Provider for publication name of Maven.
     */
    val mavenPublicationNameProvider: Provider<String>
        get() = mavenPublicationName

    /**
     *  Publishing name of Maven of the project.
     */
    var mavenPublicationName: Property<String> = objectFactory.property(String::class.java)

    init {
        mavenPublicationName.convention("mvn")
    }
}
