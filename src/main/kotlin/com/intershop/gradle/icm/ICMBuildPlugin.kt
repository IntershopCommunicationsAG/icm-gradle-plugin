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
import com.intershop.gradle.icm.tasks.CreateServerInfoProperties
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The main plugin class of this plugin.
 */
class ICMBuildPlugin : Plugin<Project> {

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
                dbinit.extendsFrom(configurations.getByName("Implementation"))
                dbinit.setTransitive(false)

                val icmserver = configurations.maybeCreate("icmserver")
                icmserver.setTransitive(false)

                configurations.maybeCreate("runtimeLib")
                configurations.maybeCreate("dockerRuntimeLib")

                configureCreateServerInfoPropertiesTask(project, extension)
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
