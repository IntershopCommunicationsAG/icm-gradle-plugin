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
import java.util.*
import javax.inject.Inject

/**
 * Extends the extension with important
 * file directories for the server.
 *
 * @constructor creates a configuration from environment variables.
 */
open class DevelopmentConfiguration
    @Inject constructor(objectFactory: ObjectFactory, providerFactory: ProviderFactory) {

    private val logger: Logger = LoggerFactory.getLogger(DevelopmentConfiguration::class.java)

    companion object {
        /**
         * Logger instance for logging.
         */
        val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        const val LICENSE_DIR_ENV = "LICENSEDIR"
        const val CONFIG_DIR_ENV = "CONFIGDIR"

        const val LICENSE_DIR_SYS = "licenseDir"
        const val CONFIG_DIR_SYS = "configDir"

        const val DEFAULT_LIC_PATH = "icm-default/lic"
        const val DEFAULT_CONFIG_PATH = "icm-default/conf"

        const val LICENSE_FILE_NAME = "license.xml"
        const val CONFIG_FILE_NAME = "cluster.properties"
    }

    private val licenseDirectoryProperty: Property<String> = objectFactory.property(String::class.java)
    private val configDirectoryProperty: Property<String> = objectFactory.property(String::class.java)
    private val configProperties: Properties = Properties()

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

        val configFile = File(configDirectory, CONFIG_FILE_NAME)
        if(configFile.exists() && configFile.canRead()) {
            configProperties.load(configFile.inputStream())
        } else {
            logger.warn("File '{}' does not exists!", configFile.absolutePath)
        }
    }



    /**
     * License directory path of the project.
     */
    val licenseDirectory: String
        get() = licenseDirectoryProperty.get()

    val licenseFilePath: String
        get() = File(licenseDirectory, LICENSE_FILE_NAME).absolutePath

    /**
     * Local configuration path of the project.
     */
    val configDirectory: String
        get() = configDirectoryProperty.get()

    val configFilePath: String
        get() = File(configDirectory, CONFIG_FILE_NAME).absolutePath

    fun getConfigProperty(property: String): String {
        return configProperties.getProperty(property, "")
    }
}
