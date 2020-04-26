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
import com.intershop.gradle.icm.utils.CartridgeStyle
import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.util.*
import javax.inject.Inject

open class ExtendCartridgeList @Inject constructor(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory) : DefaultTask() {

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

    @get:Input
    val cartridges: SetProperty<String> = objectFactory.setProperty(String::class.java)

    fun provideCartridges(list: Provider<Set<String>>) = cartridges.set(list)

    @get:Input
    val dbprepareCartridges: SetProperty<String> = objectFactory.setProperty(String::class.java)

    fun provideDBprepareCartridges(list: Provider<Set<String>>) = dbprepareCartridges.set(list)

    @get:InputFile
    val templateFile: RegularFileProperty = objectFactory.fileProperty()

    fun provideTemplateFile(file: Provider<RegularFile>) = templateFile.set(file)

    @get:Optional
    @get:Input
    val environmentTypes: ListProperty<EnvironmentType> = objectFactory.listProperty(EnvironmentType::class.java)

    fun provideEnvironmentTypes(list: Provider<List<EnvironmentType>>) = environmentTypes.set(list)

    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    fun provideOutputFile(file: Provider<RegularFile>) = outputFile.set(file)

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Extend an existing cartrldge list."

        outputFile.convention(projectLayout.buildDirectory.file("cartridgelist/${CARTRIDGELISTFILE_NAME}"))
        environmentTypes.convention(listOf(EnvironmentType.PRODUCTION))
    }

    @Throws(GradleException::class)
    @TaskAction
    fun extendList() {

        val prjCartridgeSet = mutableSetOf<String>()
        val prjInitCartridgeSet = mutableSetOf<String>()
        val projectCartridgeMap = getSubProjectStyleMap()

        cartridges.get().forEach { cartridge ->
            val cn = getCartridgeNameFrom(cartridge, projectCartridgeMap)
            if(cn != null) {
                prjCartridgeSet.add(cn)
            } else {
                project.logger.debug("{} is not part of the property '{}'", cartridge, CARTRIDGES_PROPERTY)
            }
        }

        dbprepareCartridges.get().forEach { cartridge ->
            val cartridgeModule = cartridge.split(":")
            val cartridgeName = if(cartridgeModule.size > 1) { cartridgeModule[1] } else { cartridge }

            val cn = if(prjCartridgeSet.contains(cartridgeName)) {
                        cartridgeName
                     } else {
                         getCartridgeNameFrom(cartridge, projectCartridgeMap)
                     }
            if(cn != null) {
                prjInitCartridgeSet.add(cn)
            } else {
                project.logger.debug("{} is not part of the property '{}'", cartridge, CARTRIDGES_DBINIT_PROPERTY)
            }
        }

        val props = readProperties()
        val cartridgeProp = props.getProperty(CARTRIDGES_PROPERTY)
            ?: throw GradleException(
                "There is no list of cartridges in (" + templateFile.get().asFile.absolutePath +")")

        val dbinitCartridgeProp = props.getProperty(CARTRIDGES_DBINIT_PROPERTY)
            ?: throw GradleException(
                "There is no list of dbinit cartridges in (" + templateFile.get().asFile.absolutePath +")")

        val cartridgeSet: MutableSet<String> = cartridgeProp.split(" ").toMutableSet()
        val initCartridgeSet: MutableSet<String> =  dbinitCartridgeProp.split(" ").toMutableSet()

        cartridgeSet.addAll(prjCartridgeSet)
        initCartridgeSet.addAll(prjInitCartridgeSet)

        writeProperties(cartridgeSet, initCartridgeSet)
    }

    private fun getSubProjectStyleMap(): Map<String, CartridgeStyle> {
        val projectCartridgeMap = mutableMapOf<String, CartridgeStyle>()

        project.rootProject.subprojects { prj ->
            if(prj.hasProperty("cartridge.style")) {
                projectCartridgeMap[prj.name] = CartridgeStyle.valueOf(prj.property("cartridge.style").toString().toUpperCase())
            }
        }

        return  projectCartridgeMap
    }

    private fun getCartridgeNameFrom(cartridge: String, projectMap: Map<String, CartridgeStyle>): String? {
        if(CartridgeUtil.isModuleDependency(cartridge)) {
            val cartridgeModule = cartridge.split(":")

            if(CartridgeUtil.isCartridge(project,
                    cartridgeModule[0], cartridgeModule[1], cartridgeModule[2],
                    environmentTypes.get())) {
                return cartridgeModule[1]
            }
        } else {
            val style = projectMap[cartridge]
            if( style != null && environmentTypes.get().contains(style.environmentType())) {
                return cartridge
            }
        }
        return null
    }

    @Throws(GradleException::class)
    private fun readProperties(): Properties {
        val orgProps = Properties()
        try {
            orgProps.load(templateFile.get().asFile.bufferedReader())
            return orgProps
        } catch(ioex: IOException) {
            throw GradleException(
                "Can not read orignal cartridge properies (" + templateFile.get().asFile.absolutePath +")")
        }
    }

    private fun writeProperties(cartridgeSet: Set<String>, initCartridgeSet: Set<String>) {
        val propFile = outputFile.get().asFile

        propFile.printWriter().use { out ->
            out.println("# generated cartridge list properties for '${project.name}'")
            out.println("${CARTRIDGES_PROPERTY} = \\")
            cartridgeSet.forEach {
                if(it == cartridgeSet.last()) {
                    out.println("\t${it}")
                } else {
                    out.println("\t${it} \\")
                }
            }
            out.println("")
            out.println("${CARTRIDGES_DBINIT_PROPERTY} = \\")
            initCartridgeSet.forEach {
                if(it == initCartridgeSet.last()) {
                    out.println("\t${it}")
                } else {
                    out.println("\t${it} \\")
                }
            }
        }

        project.logger.info("cartridgelist.properties are written to {}", outputFile)
    }
}