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
package com.intershop.gradle.icm

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.CopyThirdpartyLibs
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import com.intershop.gradle.icm.tasks.WriteCartridgeClasspath
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.JavaPlugin
import java.io.File

/**
 * The main plugin class of this plugin.
 */
class ICMBuildPlugin : Plugin<Project> {

    companion object {
        const val PROJECT_INFO_FILE = "version.properties"
        const val PROJECT_INFO_DIR = "serverInfoProps"

        const val THIRDPARTYLIB_DIR = "lib"

        const val CARTRIDGE_DESCRIPTOR_DIR = "descriptor"
        const val CARTRIDGE_DESCRIPTOR_FILE = "cartridge.descriptor"

        const val CARTRIDGE_CLASSPATH_DIR = "classpath"
        const val CARTRIDGE_CLASSPATH_FILE = "cartridge.classpath"
    }

    override fun apply(project: Project) {
        with(project) {
            if(project.rootProject == project) {
                logger.info(
                    "ICM build plugin adds extension {} to {}",
                    IntershopExtension.INTERSHOP_EXTENSION_NAME,
                    name
                )

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(
                    IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java, this
                )

                // create configurations for ICM project
                val dbinit = configurations.maybeCreate("dbinit")
                dbinit.setTransitive(false)

                val icmserver = configurations.maybeCreate("icmserver")
                icmserver.setTransitive(false)

                configurations.maybeCreate("runtimeLib")
                configurations.maybeCreate("dockerRuntimeLib")

                configureCreateServerInfoPropertiesTask(project, extension)

                rootProject.subprojects.forEach { prj ->
                    prj.plugins.withType(JavaPlugin::class.java) {

                        var implementation = prj.configurations.findByName("implementation");

                        val cartridge = prj.configurations.maybeCreate("cartridge")
                        cartridge.setTransitive(false)
                        if(implementation != null) {
                            implementation.extendsFrom(cartridge)
                        }

                        val cartridgeRuntime = prj.configurations.maybeCreate("cartridgeRuntime")
                        cartridgeRuntime.extendsFrom(cartridge)
                        cartridgeRuntime.setTransitive(true)

                        prj.tasks.maybeCreate("copyThirdpartyLibs", CopyThirdpartyLibs::class.java)
                        var descriptorTask = prj.tasks.maybeCreate("writeCartridgeDescriptor",
                            WriteCartridgeDescriptor::class.java).apply {
                            dependsOn(cartridgeRuntime)
                        }
                        var classpathTask = prj.tasks.maybeCreate("writeCartridgeClasspath",
                            WriteCartridgeClasspath::class.java)

                        var jarTask = prj.tasks.findByName("jar")
                        if(jarTask != null) {
                            jarTask.dependsOn( descriptorTask )
                            jarTask.dependsOn( classpathTask )
                        }
                     }
                }
            } else {
                logger.warn("ICM build plugin will be not applied to the sub project '{}'", project.name)
            }
        }
    }

    private fun configureCreateServerInfoPropertiesTask(project: Project, extension: IntershopExtension) {
        with(project) {
            tasks.maybeCreate("createServerInfoProperties", CreateServerInfoProperties::class.java).apply {
                provideProductId(extension.projectInfo.productIDProvider)
                provideProductName(extension.projectInfo.productNameProvider)
                provideCopyrightOwner(extension.projectInfo.copyrightOwnerProvider)
                provideCopyrightFrom(extension.projectInfo.copyrightFromProvider)
                provideOrganization(extension.projectInfo.organizationProvider)
            }
        }
    }
}
