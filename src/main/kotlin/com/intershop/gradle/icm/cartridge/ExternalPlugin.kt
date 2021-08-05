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
package com.intershop.gradle.icm.cartridge

import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import com.intershop.gradle.icm.tasks.ZipStaticFiles
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * The external cartridge plugin applies all basic configurations
 * and tasks for a cartridge project, that can be provided as
 * module dependency to other projects.
 */
open class ExternalPlugin : Plugin<Project> {

    companion object {
        const val TASK_ZIPSTATICFILES = "zipStaticFiles"
    }

    override fun apply(project: Project) {
        with(project) {
            plugins.apply(PublicPlugin::class.java)

            val extension = rootProject.extensions.getByType(IntershopExtension::class.java)
            val descriptorTask = this.tasks.named(
                WriteCartridgeDescriptor.DEFAULT_NAME,
                WriteCartridgeDescriptor::class.java)

            try {
                this.tasks.named(TASK_ZIPSTATICFILES, ZipStaticFiles::class.java)
            } catch (ex: UnknownTaskException) {
                this.tasks.register(TASK_ZIPSTATICFILES, ZipStaticFiles::class.java)
            }

            val zipStaticTask = tasks.named(TASK_ZIPSTATICFILES, ZipStaticFiles::class.java)
            zipStaticTask.configure { task ->
                task.from( project.provider { descriptorTask.get().outputFile } )
                task.dependsOn(descriptorTask)
            }

            with(extensions) {
                plugins.withType(MavenPublishPlugin::class.java) {
                    configure(PublishingExtension::class.java) { publishing ->
                        publishing.publications.maybeCreate(
                            extension.mavenPublicationName.get(),
                            MavenPublication::class.java
                        ).apply {
                            artifact(zipStaticTask.get())
                            pom.properties.put("cartridge.type", "external")
                        }
                    }
                    tasks.named("publish").configure { task -> task.dependsOn(zipStaticTask) }
                }
            }
        }
    }
}
