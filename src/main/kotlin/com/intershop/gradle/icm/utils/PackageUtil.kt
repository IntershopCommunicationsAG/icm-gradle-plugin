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

package com.intershop.gradle.icm.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration
import java.io.File

object PackageUtil {

    fun downloadPackage(project: Project, dependency: String, classifier: String): File? {

        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependency) as ExternalModuleDependency
        dep.artifact {
            it.name = dep.name
            it.classifier = classifier
            it.extension = "zip"
            it.type = "zip"
        }

        val configuration = project.configurations.detachedConfiguration(dep)
        configuration.setVisible(false)
            .setTransitive(false)
            .setDescription("$classifier for package download: $dependency")
            .defaultDependencies { ds ->
                ds.add(dep)
            }

        try {
            val files = configuration.resolve()
            return files.first()
        } catch (anfe: DefaultLenientConfiguration.ArtifactResolveException) {
            project.logger.warn("No package '{}' is available!", classifier)
        }

        return null
    }

}