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

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.extension.ProjectConfiguration
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

abstract class ExtendCartridgeList @Inject constructor(
    private var projectLayout: ProjectLayout,
    private var objectFactory: ObjectFactory) : DefaultTask() {

    companion object {


        /**
         * Path for original cartridge list properties in build directory.
         */
        const val CARTRIDGELISTFILE_NAME = "cartridgelist.properties"

        /**
         * Property name for cartridges.
         */
        const val CARTRIDGES_PROPERTY = "cartridges"

        /**
         * Property name for dbinit/dbprepare cartridges.
         */
        const val CARTRIDGES_DBINIT_PROPERTY = "cartridges.dbinit"
    }

    private val outputFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val cartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val dbprepareCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val cartridgePropertiesFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val productionCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val writeAllCartridgeListProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)


    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Creates a cluster ID to start a server."

        outputFileProperty.set(
            projectLayout.buildDirectory.file(
                "${this.name.toLowerCase()}/${CARTRIDGELISTFILE_NAME}"))
        cartridgePropertiesFileProperty.convention(
            projectLayout.buildDirectory.file(
                ProjectConfiguration.CARTRIDGELISTFILE_PATH))
        writeAllCartridgeListProperty.set(false)
    }

    /**
     * Cartridge list to extend the original cartridge list configuration.
     *
     * @property cartridges additional cartridge list
     */
    @get:Input
    var cartridges by cartridgesProperty

    /**
     * Provider to configure cartridge list to extend the original cartridge list configuration.
     *
     * @param cartridges additional cartridge list
     */
    fun provideCartridges(cartridges: Provider<Set<String>>) = cartridgesProperty.set(cartridges)

    /**
     * Cartridge list to extend the original dbprepare /dbinit cartridge list configuration.
     *
     * @property dbprepareCartridges additional cartridge list
     */
    @get:Input
    var dbprepareCartridges by dbprepareCartridgesProperty

    /**
     * Provider to configure cartridge list to extend the original dbprepare /dbinit cartridge list configuration.
     *
     * @param cartridges additional cartridge list
     */
    fun provideDBprepareCartridges(cartridges: Provider<Set<String>>) = dbprepareCartridgesProperty.set(cartridges)

    /**
     * Filter for cartridges and dbprepareCartridges. All listed cartridges are production cartridges.
     * Not listed cartridges are test or development cartridges. If the list is empty all cartrdiges
     * are production catridges.
     *
     * @property productionCartridges cartridge list of production cartridges
     */
    @get:Input
    var productionCartridges by productionCartridgesProperty

    /**
     * Provider to configure production cartridge filter.
     *
     * @param cartridges cartridge list
     */
    fun provideProductionCartridges(cartridges: Provider<Set<String>>) = productionCartridgesProperty.set(cartridges)

    /**
     * If writeAllCartridgeList property is true
     * the configuration is written with all cartridges.
     *
     * @property writeAllCartridgeList
     */
    @get:Input
    var writeAllCartridgeList by writeAllCartridgeListProperty

    /**
     * Configure provider for writeAllCartridgeListProperty.
     *
     * @param writeAllCartridgeList
     */
    fun provideWriteAllCartridgeList(writeAllCartridgeList: Provider<Boolean>)
                = writeAllCartridgeListProperty.set(writeAllCartridgeList)


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

    /**
     * Provides an input file for this task.
     *
     * @param cartridgePropertiesFile
     */
    fun provideCartridgePropertiesFile(cartridgePropertiesFile: Provider<RegularFile>)
            = cartridgePropertiesFileProperty.set(cartridgePropertiesFile)

    /**
     * Input template file with all cartridges.
     *
     * @property cartridgePropertiesFile
     */
    @get:InputFile
    var cartridgePropertiesFile: File
        get() = cartridgePropertiesFileProperty.get().asFile
        set(value) = cartridgePropertiesFileProperty.set(value)

    /**
     * This function represents the logic of this task.
     */
    @Throws(GradleException::class)
    @TaskAction
    fun extendCartridgeList() {
        if(! cartridgePropertiesFile.exists()) {
            throw GradleException(
                "Orignal cartridge properties does not exists (" + cartridgePropertiesFile.absolutePath +")")
        }
        val orgProps = Properties()
        try {
            orgProps.load(cartridgePropertiesFile.bufferedReader())
        } catch(ioex: IOException) {
            throw GradleException(
                "Can not read orignal cartridge properies (" + cartridgePropertiesFile.absolutePath +")")
        }

        val cartridgeProp = orgProps[CARTRIDGES_PROPERTY]
            ?: throw GradleException(
                "There is no list of cartridges in (" + cartridgePropertiesFile.absolutePath +")")

        val dbinitCartridgeProp = orgProps[CARTRIDGES_DBINIT_PROPERTY]
            ?: throw GradleException(
                "There is no list of dbinit cartridges in (" + cartridgePropertiesFile.absolutePath +")")

        project.logger.info("Data of original cartridgelist.properties read from {}", cartridgePropertiesFile)

        val cartridgesSet: MutableSet<String> = cartridgeProp.toString().split(" ").toMutableSet()
        val initCartridgesSet: MutableSet<String> =  dbinitCartridgeProp.toString().split(" ").toMutableSet()

        cartridges.forEach {
            if (productionCartridges.isEmpty() || productionCartridges.contains(it) || writeAllCartridgeList) {
                cartridgesSet.add(it)
            }
        }
        dbprepareCartridges.forEach {
            if (productionCartridges.isEmpty() || productionCartridges.contains(it) || writeAllCartridgeList) {
                initCartridgesSet.add(it)
            }
        }

        project.logger.info("Create data for cartridgelist.properties are written")

        // write cartridge list
        if(outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.parentFile.mkdir()

        outputFile.printWriter().use { out ->
            out.println("# generated cartridge list properties for '${project.name}'")
            out.println("cartridges= \\")
            cartridgesSet.forEach {
                if(it == cartridgesSet.last()) {
                    out.println("\t${it}")
                } else {
                    out.println("\t${it} \\")
                }
            }
            out.println("")
            out.println("cartridges.dbinit= \\")
            initCartridgesSet.forEach {
                if(it == initCartridgesSet.last()) {
                    out.println("\t${it}")
                } else {
                    out.println("\t${it} \\")
                }
            }
        }

        project.logger.info("cartridgelist.properties are written to {}", outputFile)
    }
}
