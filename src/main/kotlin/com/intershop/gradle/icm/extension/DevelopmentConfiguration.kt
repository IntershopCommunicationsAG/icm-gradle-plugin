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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.wrapper.GradleUserHomeLookup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

abstract class DevelopmentConfiguration {

    companion object {
        /**
         * Logger instance for logging.
         */
        val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        const val LICENSE_DIR_ENV = "LICENSEDIR"
        const val CONFIG_DIR_ENV = "CONFIGDIR"

        const val LICENSE_DIR_SYS = "licenseDir"
        const val CONFIG_DIR_SYS = "configDir"

        const val GRADLE_USER_HOME = "GRADLE_USER_HOME"

        const val DEFAULT_LIC_PATH = "icm-default/lic"
        const val DEFAULT_CONFIG_PATH = "icm-default/conf"
    }

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    /**
     * Inject service of ProviderFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val providerFactory: ProviderFactory

    private val licenseDirectoryProperty: Property<String> = objectFactory.property(String::class.java)
    private val configDirectoryProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        // read environment
        val gradleUserHomePath = GradleUserHomeLookup.gradleUserHome().absolutePath

        var licDirPath = providerFactory.environmentVariable(LICENSE_DIR_ENV).orNull
        var configDirPath = providerFactory.environmentVariable(CONFIG_DIR_ENV).orNull

        // read system if necessary
        if(licDirPath == null) {
            licDirPath = providerFactory.systemProperty(LICENSE_DIR_SYS).orNull
        }

        if(configDirPath == null) {
            configDirPath = providerFactory.systemProperty(CONFIG_DIR_SYS).orNull
        }

        if(licDirPath == null) {
            try {
                licDirPath = providerFactory.gradleProperty(LICENSE_DIR_SYS).orNull
            } catch ( ise: IllegalStateException ) {
                log.error(ise.message)
            }
        }

        if(configDirPath == null) {
            try {
                configDirPath = providerFactory.gradleProperty(CONFIG_DIR_SYS).orNull
            } catch ( ise: IllegalStateException ) {
                log.error(ise.message)
            }
        }

        if(licDirPath == null) {
            licDirPath = File(File(gradleUserHomePath), DEFAULT_LIC_PATH).absolutePath
        }

        if(configDirPath == null) {
            configDirPath = File(File(gradleUserHomePath), DEFAULT_CONFIG_PATH).absolutePath
        }

        licenseDirectoryProperty.set(licDirPath)
        configDirectoryProperty.set(configDirPath)
    }

    /**
     * License directory path of the project.
     */
    val licenseDirectory
        get() = licenseDirectoryProperty.get()

    val licenseFilePath
        get() = File(licenseDirectory, "license.xml").absolutePath

    /**
     * Local configuration path of the project.
     */
    val configDirectory
        get() = configDirectoryProperty.get()

    val confgiFilePath
        get() = File(configDirectory, "cluster.properties").absolutePath
}
