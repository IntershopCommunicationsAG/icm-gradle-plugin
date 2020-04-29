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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * Extension to configure sub projects, like connectors,
 * payment provider etc.
 */
open class CartridgeProject @Inject constructor(objectFactory: ObjectFactory) {

    /**
     * Dependency of the base project.
     *
     * @property dependency
     */
    @get:Input
    val dependency: Property<String> = objectFactory.property(String::class.java)

    /**
     * Configuration for configuration package.
     *
     * @property configPackage
     */
    @get:Nested
    val configPackage : FilePackage = objectFactory.newInstance(FilePackage::class.java)

    fun configPackage(action: Action<in FilePackage>) {
        action.execute(configPackage)
    }

    fun configPackage(c: Closure<FilePackage>) {
        ConfigureUtil.configure(c, configPackage)
    }

    /**
     * Configuration for sites package.
     *
     * @property sitesPackage
     */
    @get:Nested
    val sitesPackage : FilePackage = objectFactory.newInstance(FilePackage::class.java)

    fun sitesPackage(action: Action<in FilePackage>) {
        action.execute(sitesPackage)
    }

    fun sitesPackage(c: Closure<FilePackage>) {
        ConfigureUtil.configure(c, sitesPackage)
    }
}
