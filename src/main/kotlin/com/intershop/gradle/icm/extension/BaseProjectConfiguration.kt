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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * Extension to configure sub projects, like connectors,
 * payment provider etc.
 */
abstract class BaseProjectConfiguration( @get:Internal val name: String ) {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val dependencyProperty: Property<String> = objectFactory.property(String::class.java)
    private val withCartridgeListProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)

    init {
        withCartridgeListProperty.set(false)
    }

    /**
     * Dependency of the base project.
     *
     * @property dependency
     */
    @get:Input
    var dependency by dependencyProperty

    /**
     * If the configuration contains a cartridge
     * list properties this variable is true.
     *
     * @property withCartridgeList
     */
    @get:Input
    var withCartridgeList by withCartridgeListProperty

    /**
     * Configuration for configuration package.
     *
     * @property confPackage
     */
    @get:Optional
    @get:Nested
    val confPackage : PackageConf = objectFactory.newInstance(PackageConf::class.java)

    /**
     * Configures confPackage with an action.
     *
     * @param action
     */
    fun confPackage(action: Action<in PackageConf>) {
        action.execute(confPackage)
    }

    /**
     * Configures confPackage with a closure.
     *
     * @param c
     */
    fun confPackage(c: Closure<PackageConf>) {
        ConfigureUtil.configure(c, confPackage)
    }

    /**
     * Configuration for sites package.
     *
     * @property sitesPackage
     */
    @get:Optional
    @get:Nested
    val sitesPackage : PackageConf = objectFactory.newInstance(PackageConf::class.java)

    /**
     * Configures sitesPackage with an action.
     *
     * @param action
     */
    fun sitesPackage(action: Action<in PackageConf>) {
        action.execute(sitesPackage)
    }

    /**
     * Configures sitesPackage with a closure.
     *
     * @param c
     */
    fun sitesPackage(c: Closure<PackageConf>) {
        ConfigureUtil.configure(c, sitesPackage)
    }
}
