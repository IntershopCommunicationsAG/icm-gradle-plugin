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
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import com.intershop.gradle.icm.tasks.ZipStaticFiles
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

/**
 * The project cartridge plugin applies all basic configurations
 * and tasks for a cartridge project, that can be provided as
 * module dependendcy to other projects.
 */
open class ProjectCartridgePlugin : Plugin<Project> {

    companion object {
        const val TASK_ZIPSTATICFILES = "zipStaticFiles"
    }

    override fun apply(project: Project) {
        with(project) {

            plugins.apply(CartridgePlugin::class.java)

            val extension = rootProject.extensions.getByType(IntershopExtension::class.java)

            var zipStaticTask = tasks.findByName(TASK_ZIPSTATICFILES)
            var descriptorTask = tasks.getByName(WriteCartridgeDescriptor.DEFAULT_NAME)

            if( zipStaticTask == null) {
                zipStaticTask = tasks.create(TASK_ZIPSTATICFILES, ZipStaticFiles::class.java) {
                    it.from(descriptorTask.outputs.files)
                }
            }

            extensions.configure(PublishingExtension::class.java) { publishing ->
                publishing.publications.maybeCreate(
                    extension.mavenPublicationName,
                    MavenPublication::class.java
                ).apply {
                    artifact(zipStaticTask)

                    pom.properties.put("cartridge.type", "optional")
                }
            }

            tasks.getByName("publish").dependsOn(zipStaticTask)
        }
    }
}
