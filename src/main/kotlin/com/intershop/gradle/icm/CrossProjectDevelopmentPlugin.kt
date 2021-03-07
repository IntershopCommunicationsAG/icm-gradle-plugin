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

import com.intershop.gradle.icm.tasks.crossproject.WriteMappingFile
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Special plugin for Intershop internal development.
 */
class CrossProjectDevelopmentPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            if (project.rootProject == this) {
                logger.info("ICM Cross-Project Development plugin will be initialized")

                this.tasks.register("writeMapping", WriteMappingFile::class.java).configure {
                    group = "ICM Cross-Project Development"
                    description = "Writes settings.gradle.kts file for composite builds"
                }
            }
        }
    }
}
