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
package com.intershop.gradle.icm.tasks

import com.intershop.gradle.icm.extension.IntershopExtension.Companion.INTERSHOP_GROUP_NAME
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.util.PropertiesUtils
import java.io.IOException
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * CreateServerInfoProperties Gradle task 'createServerInfoProperties'
 *
 * This task creates a properties file with all project
 * information. This property is used by the server.
 */
open class CreateServerInfo @Inject constructor(
        projectLayout: ProjectLayout,
        objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "createServerInfo"

        const val VERSIONINFO_FILENAME = "version.properties"
        const val VERSIONINFO_FOLDER = "versioninfo"
        const val VERSIONINFO = "$VERSIONINFO_FOLDER/$VERSIONINFO_FILENAME"

        private val now = LocalDateTime.now()
        val dateTime: String = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val year: String = now.format(DateTimeFormatter.ofPattern("yyyy"))
    }

    @get:Input
    val productId: Property<String> = objectFactory.property(String::class.java)

    /**
     * Configure product id provider for this task.
     *
     * @property productId provider for product ID
     */
    fun provideProductId(id: Provider<String>) = productId.set(id)

    @get:Input
    val productName: Property<String> = objectFactory.property(String::class.java)

    /**
     * Configure product name provider for this task.
     *
     * @property productName provider for product name
     */
    fun provideProductName(name: Provider<String>) = productName.set(name)

    @get:Input
    val copyrightOwner: Property<String> = objectFactory.property(String::class.java)

    /**
     * Configure copyright owner provider for this task.
     *
     * @property copyrightOwner provider for copyright owner property
     */
    fun provideCopyrightOwner(owner: Provider<String>) = copyrightOwner.set(owner)

    @get:Input
    val copyrightFrom: Property<String> = objectFactory.property(String::class.java)

    /**
     * Configure copyright from provider for this task.
     *
     * @property copyrightFrom provider for copyright from property
     */
    fun provideCopyrightFrom(from: Provider<String>) = copyrightFrom.set(from)

    @get:Input
    val organization: Property<String> = objectFactory.property(String::class.java)

    /**
     * Configure organization provider for this task.
     *
     * @property organization provider for organization
     */
    fun provideOrganization(org: Provider<String>) = organization.set(org)

    /**
     * Output file for generated cluster id.
     *
     * @property outputFile
     */
    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Provides an output file for this task.
     *
     * @param file
     */
    fun provideOutputfile(file: Provider<RegularFile>) = outputFile.set(file)

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Writes the server information to a file."

        outputFile.convention(projectLayout.buildDirectory.file(VERSIONINFO))
    }

    /**
     * Main function to run the task functionality.
     */
    @Throws(IOException::class)
    @TaskAction
    fun writeProperties(){

        if(! outputFile.get().asFile.parentFile.exists()) {
            outputFile.get().asFile.parentFile.mkdirs()
        }

        val props = linkedMapOf<String,String>()
        val comment = "Generated - during build for server"

        props["version.information.version"] = project.version.toString()
        props["version.information.installationDate"] = dateTime

        props["version.information.productId"] = productId.getOrElse("product id")
        props["version.information.productName"] = productName.getOrElse(project.name)
        props["version.information.copyrightOwner"] = copyrightOwner.getOrElse("Intershop Communications")
        props["version.information.copyrightFrom"] = copyrightFrom.getOrElse("2021")

        props["version.information.copyrightTo"] = year
        props["version.information.organization"] = organization.getOrElse("Intershop Communications")

        val propsObject = Properties()
        propsObject.putAll(props)
        try {
            PropertiesUtils.store(
                propsObject,
                outputFile.get().asFile,
                comment,
                Charset.forName("ISO_8859_1"),
                "\n"
            )
        } finally {
            project.logger.debug("Write properties finished.")
        }
    }
}
