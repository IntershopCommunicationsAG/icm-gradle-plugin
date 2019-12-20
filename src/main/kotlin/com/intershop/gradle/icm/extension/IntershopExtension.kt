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
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Extension for ICM properties.
 */
open class IntershopExtension(var project: Project)  {

    companion object {
        // names for the plugin
        const val INTERSHOP_EXTENSION_NAME = "intershop"
        const val INTERSHOP_GROUP_NAME = "Intershop Commerce Management"
    }

    private val mavenPublicationNameProperty: Property<String> = project.objects.property(String::class.java)

    val projectInfo: ProjectInfo = ProjectInfo(project)
    val baseConfig: BaseConfiguration = BaseConfiguration(project)

    init {
        mavenPublicationNameProperty.convention("mvn")
    }

    /**
     * Configures the project information configuration.
     *
     * @param closure closure with project information configuration
     */
    @Suppress("unused")
    fun projectInfo(closure: Closure<Any>) {
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

    /**
     * Configures the base project of Intershop Commerce Management.
     *
     * @param closure closure with base project configuration of Intershop Commerce Management
     */
    @Suppress("unused")
    fun baseConfig(closure: Closure<Any>) {
        project.configure(baseConfig, closure)
    }

    /**
     * Configures the base project of Intershop Commerce Management.
     *
     * @param action action with base project configuration of Intershop Commerce Management
     */
    fun baseConfig(action: Action<in BaseConfiguration>) {
        action.execute(baseConfig)
    }

    /**
     * Provider for publication name of Maven.
     */
    val mavenPublicationNameProvider: Provider<String>
        get() = mavenPublicationNameProperty

    /**
     *  Publishing name of Maven of the project.
     */
    var mavenPublicationName by mavenPublicationNameProperty

}
