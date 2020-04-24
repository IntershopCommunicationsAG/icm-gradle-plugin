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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension to configure sub projects, like connectors,
 * payment provider etc.
 */
abstract class CartridgeProject {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val dependencyProperty: Property<String> = objectFactory.property(String::class.java)

    /**
     * Dependency of the base project.
     *
     * @property dependency
     */

    @get:Input
    var dependency by dependencyProperty

    val dependencyProvider: Provider<String>
        get() = dependencyProperty

    /**
     * Configuration for configuration package.
     *
     * @property configPackage
     */
    @get:Nested
    val configPackage : FilePackage = objectFactory.newInstance(FilePackage::class.java)


    /**
     * Configuration for sites package.
     *
     * @property sitesPackage
     */
    @get:Nested
    val sitesPackage : FilePackage = objectFactory.newInstance(FilePackage::class.java)
}
