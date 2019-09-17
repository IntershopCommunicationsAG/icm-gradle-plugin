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
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Extension needed for the configuration of an INTERSHOP base project.
 */
class BaseConfiguration(project: Project) {

    private val runtimeModuleProperty: Property<String> = project.objects.property(String::class.java)
    private val runtimeVersionProperty: Property<String> = project.objects.property(String::class.java)
    private val configurationFolderTaskPathProperty: Property<String> = project.objects.property(String::class.java)
    private val sitesFolderTaskPathProperty: Property<String> = project.objects.property(String::class.java)

    init {
        runtimeModuleProperty.set("com.intershop.platform.lib:runtime-lib")
        runtimeVersionProperty.set("1.0.0")
    }

    /**
     * Runtime module property provider of ICM base project.
     */
    val runtimeModuleProvider: Provider<String>
        get() = runtimeModuleProperty

    /**
     * Runtime module of ICM base project.
     */
    var runtimeModule by runtimeModuleProperty

    /**
     * Runtime version property provider of ICM base project.
     */
    val runtimeVersionProvider: Provider<String>
        get() = runtimeVersionProperty

    /**
     * Runtime version of ICM base project.
     */
    var runtimeVersion by runtimeVersionProperty

    /**
     * Task path property provider of task which creates the ICM configuration folder of the ICM base project.
     */
    val configurationFolderTaskPathProvider: Provider<String>
        get() = configurationFolderTaskPathProperty

    /**
     * Task path property of task which creates the ICM configuration folder of the ICM base project.
     */
    var configurationFolderTaskPath by configurationFolderTaskPathProperty

    /**
     * Task path property provider of task which creates the ICM sites folder of the ICM base project.
     */
    val sitesFolderTaskPathProvider: Provider<String>
        get() = sitesFolderTaskPathProperty

    /**
     * Task path property of task which creates the ICM sites folder of the ICM base project.
     */
    var sitesFolderTaskPath by sitesFolderTaskPathProperty
}
