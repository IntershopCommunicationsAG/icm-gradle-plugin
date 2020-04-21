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

import com.intershop.gradle.icm.extension.CartridgeProject
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Task to create configuration folder.
 */
open class CreateConfFolder @Inject constructor(
    objectFactory: ObjectFactory,
    fsOps: FileSystemOperations): AbstractCreateFolder(objectFactory, fsOps) {

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

        const val CLUSTER_CONF = "system-conf/cluster"
    }

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
    }

    private val cartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val dbprepareCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val productionCartridgesProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)

    private val versionInfoFileProperty: RegularFileProperty = objectFactory.fileProperty()

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
     * Provider to configure the version information file.
     *
     * @param file version information file
     */
    fun provideVersionInfoFile(file: Provider<RegularFile>) = versionInfoFileProperty.set(file)

    @get:Optional
    @get:InputFile
    var versionInfoFile: File?
        get() = if (versionInfoFileProperty.orNull != null) versionInfoFileProperty.get().asFile else null
        set(value) = versionInfoFileProperty.set(value)

    @get:Internal
    override val classifier: String
        get() = "configuration"

    /**
     * Main function to run the task functionality.
     */
    @TaskAction
    fun runFolderCreation() {
        super.startFolderCreation()
    }

    @get:Internal
    var writeDevConf = true

    /**
     * Add copy spec for an package file.
     *
     * @param cs        base copy spec
     * @param pkgCS     package copy spec
     * @param prjConf   configuration of a base project
     * @param file      package file it self
     */
    override fun addCopyConfSpec(cs: CopySpec, pkgCS: CopySpec, prjConf: CartridgeProject, file: File) {

        with(prjConf) {

            configPackage.excludes.forEach {
                pkgCS.exclude(it)
            }
            configPackage.includes.forEach {
                pkgCS.include(it)
            }
            if(! configPackage.targetPath.isNullOrEmpty()) {
                pkgCS.into(configPackage.targetPath!!)
            }
            if(configPackage.duplicateStrategy != DuplicatesStrategy.INHERIT) {
                pkgCS.duplicatesStrategy = configPackage.duplicateStrategy
            }

           // if(withCartridgeList) {
                pkgCS.exclude("**/**/${CARTRIDGELISTFILE_NAME}")
           // }

            if(versionInfoFile != null) {
                pkgCS.exclude("**/**/version.properties")

                cs.from(versionInfoFile!!.parent) { fcs ->
                    fcs.include("**/**/version.properties")
                    fcs.into(CLUSTER_CONF)
                }
            }
/**
            if(withCartridgeList) {
                val propsFile = getCartridgeListProps(file)
                if(propsFile != null) {
                    cs.from(propsFile) { cs ->
                        cs.into(CLUSTER_CONF)
                    }
                }
            }
            **/
        }
    }

    private fun getCartridgeListProps(zipFile: File): File? {
        //val pfiles = project.zipTree(zipFile).matching { pf ->
        //    pf.include("**/**/${CARTRIDGELISTFILE_NAME}")
        //}
/**
        if (!pfiles.isEmpty) {
            val orgProps = Properties()
            try {
                orgProps.load(pfiles.first().bufferedReader())
            } catch(ioex: IOException) {
                throw GradleException(
                    "Can not read orignal cartridge properies (" + pfiles.first().absolutePath +")")
            }

            val cartridgeProp = orgProps[CARTRIDGES_PROPERTY]
                ?: throw GradleException(
                    "There is no list of cartridges in (" + pfiles.first().absolutePath +")")

            val dbinitCartridgeProp = orgProps[CARTRIDGES_DBINIT_PROPERTY]
                ?: throw GradleException(
                    "There is no list of dbinit cartridges in (" + pfiles.first().absolutePath +")")

            return calculateProperties(cartridgeProp.toString(), dbinitCartridgeProp.toString())
        }
**/
        return null
    }

    private fun calculateProperties(cartridgesProp: String, initCartridgesProp: String): File? {
        val cartridgesSet: MutableSet<String> = cartridgesProp.split(" ").toMutableSet()
        val initCartridgesSet: MutableSet<String> =  initCartridgesProp.split(" ").toMutableSet()
/**
        cartridges.forEach {
            if (productionCartridges.isEmpty() || productionCartridges.contains(it) || writeDevConf) {
                cartridgesSet.add(it)
            }
        }
        dbprepareCartridges.forEach {
            if (productionCartridges.isEmpty() || productionCartridges.contains(it) || writeDevConf) {
                initCartridgesSet.add(it)
            }
        }

        project.logger.info("Create data for cartridgelist.properties are written")

        val outputFile = File(temporaryDir, CARTRIDGELISTFILE_NAME)

        outputFile.printWriter().use { out ->
                out.println("# generated cartridge list properties for '${project.name}'")
                out.println("$CARTRIDGES_PROPERTY = \\")
                cartridgesSet.forEach {
                    if(it == cartridgesSet.last()) {
                        out.println("\t${it}")
                    } else {
                        out.println("\t${it} \\")
                    }
                }
                out.println("")
                out.println("$CARTRIDGES_DBINIT_PROPERTY = \\")
                initCartridgesSet.forEach {
                    if(it == initCartridgesSet.last()) {
                        out.println("\t${it}")
                    } else {
                        out.println("\t${it} \\")
                    }
                }
            }

        project.logger.info("cartridgelist.properties are written to {}", outputFile)
 **/
        return null

    }
}
