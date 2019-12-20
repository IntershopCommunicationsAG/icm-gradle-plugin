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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * The project cartridge plugin applies all basic configurations
 * and tasks for a cartridge project, that can be provided as
 * module dependendcy to other projects.
 */
open class DevelopmentCartridgePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            plugins.apply(AbstractCartridgePlugin::class.java)
            val extension = rootProject.extensions.getByType(IntershopExtension::class.java)

            with(extensions) {
                extraProperties.set("isDevCartridge", "true")

                plugins.withType(MavenPublishPlugin::class.java) {
                    configure(PublishingExtension::class.java) { publishing ->
                        publishing.publications.maybeCreate(
                            extension.mavenPublicationName,
                            MavenPublication::class.java
                        ).apply {
                            pom.properties.put("cartridge.type", "development")
                        }
                    }
                }
            }
        }
    }
}
