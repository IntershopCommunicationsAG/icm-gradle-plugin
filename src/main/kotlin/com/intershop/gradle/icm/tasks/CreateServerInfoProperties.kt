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

import com.intershop.gradle.icm.ICMBuildPlugin.Companion.PROJECT_INFO_DIR
import com.intershop.gradle.icm.ICMBuildPlugin.Companion.PROJECT_INFO_FILE
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.intershop.gradle.icm.setValue
import com.intershop.gradle.icm.getValue

/**
 * Task for the creation of server info properties.
 */
/**
 * CreateServerInfoProperties Gradle task 'createServerInfoProperties'
 *
 * This task creates a properties file with all project
 * information. This property is used by the server.
 */
open class CreateServerInfoProperties: WriteProperties() {

    companion object {
        private val now = LocalDateTime.now()
        val dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val year = now.format(DateTimeFormatter.ofPattern("yyyy"))
    }

    private val productIdProperty: Property<String> = project.objects.property(String::class.java)
    private val productNameProperty: Property<String> = project.objects.property(String::class.java)
    private val copyrightOwnerProperty: Property<String> = project.objects.property(String::class.java)
    private val copyrightFromProperty: Property<String> = project.objects.property(String::class.java)
    private val organizationProperty: Property<String> = project.objects.property(String::class.java)

    init {
        outputFile = File(project.buildDir, "$PROJECT_INFO_DIR/$PROJECT_INFO_FILE")
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

    @Throws(IOException::class)
    @TaskAction
    override fun writeProperties(){

        comment = "Generated - during build for server"
        property("version.information.version", project.version.toString())
        property("version.information.installationDate", dateTime)

        property("version.information.productId", productId)
        property("version.information.productName", productName)
        property("version.information.copyrightOwner", copyrightOwner)
        property("version.information.copyrightFrom", copyrightFrom)

        property("version.information.copyrightTo", year)
        property("version.information.organization", organization)

        super.writeProperties()
    }
}
