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
import org.gradle.api.file.CopySpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Extension to configure sub projects, like connectors,
 * payment provider etc.
 */
abstract class BaseProjectConfiguration(val name: String) {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val dependencyProperty: Property<String> = objectFactory.property(String::class.java)
    private val withCartridgeListProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)
    private val confCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)
    private val sitesCopySpecProperty: Property<CopySpec> = objectFactory.property(CopySpec::class.java)

    init {
        withCartridgeListProperty.set(false)
    }

    /**
     * Provider for dependency property.
     *
     * @property dependencyProvider
     */
    val dependencyProvider: Provider<String>
        get() = dependencyProperty

    /**
     * Dependency of the base project.
     *
     * @property dependency
     */
    @get:Input
    var dependency by dependencyProperty

    /**
     * CopySpec for the sites package.
     *
     * @property withCartridgeListProvider
     */
    val withCartridgeListProvider: Provider<Boolean>
        get() = withCartridgeListProperty

    /**
     * If the configuration contains a cartridge
     * list properties this variable is true
     *
     * @property withCartridgeList
     */
    @get:Input
    var withCartridgeList by withCartridgeListProperty

    /**
     * CopySpec for the configuration package.
     *
     * @property confCopySpecProvider
     */
    val confCopySpecProvider: Provider<CopySpec>
        get() = confCopySpecProperty

    /**
     * CopaSpec of configuration package of the base project.
     *
     * @property confCopySpec
     */
    @get:Optional
    @get:Input
    @get:Nested
    var confCopySpec: CopySpec?
        get() = confCopySpecProperty.orNull
        set(value) = confCopySpecProperty.set(value)


    /**
     * CopySpec for the sites package.
     *
     * @property sitesCopySpecProvider
     */
    val sitesCopySpecProvider: Provider<CopySpec>
        get() = sitesCopySpecProperty

    /**
     * CopaSpec of sites package of the base project.
     *
     * @property sitesCopySpec
     */
    @get:Optional
    @get:Input
    @get:Nested
    var sitesCopySpec: CopySpec?
        get() = sitesCopySpecProperty.orNull
        set(value) = sitesCopySpecProperty.set(value)

}
