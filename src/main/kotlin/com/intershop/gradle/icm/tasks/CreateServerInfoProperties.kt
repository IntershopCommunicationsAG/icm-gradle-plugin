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
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties
import org.gradle.internal.util.PropertiesUtils
import java.io.File
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
open class CreateServerInfoProperties @Inject constructor(
    private var projectLayout: ProjectLayout,
    private var objectFactory: ObjectFactory) : DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "createServerInfoProperties"
        const val PROJECT_INFO = "serverInfoProps/version.properties"

        private val now = LocalDateTime.now()
        val dateTime: String = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val year: String = now.format(DateTimeFormatter.ofPattern("yyyy"))
    }

    private val outputFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val productIdProperty: Property<String> = objectFactory.property(String::class.java)
    private val productNameProperty: Property<String> = objectFactory.property(String::class.java)
    private val copyrightOwnerProperty: Property<String> = objectFactory.property(String::class.java)
    private val copyrightFromProperty: Property<String> = objectFactory.property(String::class.java)
    private val organizationProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        group = INTERSHOP_GROUP_NAME
        description = "Writes the server information to a file."

        outputFileProperty.convention(projectLayout.buildDirectory.file(PROJECT_INFO))
    }

    @get:Input
    var productId by productIdProperty

    /**
     * Configure product id provider for this task.
     *
     * @property productId provider for product ID
     */
    fun provideProductId(productId: Provider<String>) = productIdProperty.set(productId)

    @get:Input
    var productName by productNameProperty

    /**
     * Configure product name provider for this task.
     *
     * @property productName provider for product name
     */
    fun provideProductName(productName: Provider<String>) = productNameProperty.set(productName)

    @get:Input
    var copyrightOwner by copyrightOwnerProperty

    /**
     * Configure copyright owner provider for this task.
     *
     * @property copyrightOwner provider for copyright owner property
     */
    fun provideCopyrightOwner(copyrightOwner: Provider<String>) = copyrightOwnerProperty.set(copyrightOwner)

    @get:Input
    var copyrightFrom by copyrightFromProperty

    /**
     * Configure copyright from provider for this task.
     *
     * @property copyrightFrom provider for copyright from property
     */
    fun provideCopyrightFrom(copyrightFrom: Provider<String>) = copyrightFromProperty.set(copyrightFrom)

    @get:Input
    var organization by organizationProperty

    /**
     * Configure organization provider for this task.
     *
     * @property organization provider for organization
     */
    fun provideOrganization(organization: Provider<String>) = organizationProperty.set(organization)

    /**
     * Provides an output file for this task.
     *
     * @param outputfile
     */
    fun provideOutputfile(outputfile: Provider<RegularFile>)
            = outputFileProperty.set(outputfile)

    /**
     * Output file for generated cluster id.
     *
     * @property outputFile
     */
    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)

    @Throws(IOException::class)
    @TaskAction
    fun writeProperties(){

        if(! outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        val props = linkedMapOf<String,String>()
        val comment = "Generated - during build for server"

        props["version.information.version"] = project.version.toString()
        props["version.information.installationDate"] = dateTime

        props["version.information.productId"] = productId
        props["version.information.productName"] = productName
        props["version.information.copyrightOwner"] = copyrightOwner
        props["version.information.copyrightFrom"] = copyrightFrom

        props["version.information.copyrightTo"] = year
        props["version.information.organization"] = organization

        val propsObject = Properties()
        propsObject.putAll(props)
        try {
            PropertiesUtils.store(
                propsObject,
                outputFile,
                comment,
                Charset.forName("ISO_8859_1"),
                "\n"
            )
        } finally {}
    }
}
