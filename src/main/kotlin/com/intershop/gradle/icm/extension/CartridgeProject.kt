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
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * This is part of the project extension and  describes
 * sub projects, like connectors, payment provider etc.
 *
 * @constructor provides a sub project configuration.
 */
open class CartridgeProject @Inject constructor(@Internal val project: Project, objectFactory: ObjectFactory) {

    /**
     * Dependency of the base project.
     *
     * @property dependency
     */
    @get:Input
    val dependency: Property<String> = objectFactory.property(String::class.java)

    /**
     * Image path for the project.
     *
     * @property image
     */
    @get:Input
    val image: Property<String> = objectFactory.property(String::class.java)

    /**
     * Test image path for the project.
     *
     * @property testImage
     */
    @get:Optional
    @get:Input
    val testImage: Property<String> = objectFactory.property(String::class.java)

    /**
     * Dependency of version filters of the base project.
     *
     * @property platforms
     */
    @get:Optional
    @get:Input
    val platforms: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Add a dependency to the set of dependency filter dependencies.
     *
     * @param dependency external module dependency in a short notation
     */
    fun platform(dependency: String) {
        platforms.add(dependency)
    }

    /**
     * Configuration for configuration package.
     *
     * @property configPackage
     */
    @get:Nested
    val configPackage : FilePackage = objectFactory.newInstance(FilePackage::class.java)

    /**
     * Provides the configuration of a package with configuration files.
     *
     * @param action action to configure a file package.
     */
    fun configPackage(action: Action<in FilePackage>) {
        action.execute(configPackage)
    }

    /**
     * Provides the configuration of a package with configuration files.
     *
     * @param c closure to configure a file package.
     */
    fun configPackage(c: Closure<FilePackage>) {
        project.configure(configPackage, c)
    }

}
