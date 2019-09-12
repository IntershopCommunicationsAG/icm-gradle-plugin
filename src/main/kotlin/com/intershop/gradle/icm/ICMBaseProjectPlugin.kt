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
import com.intershop.gradle.icm.utils.OsCheck
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import java.io.File

/**
 * The plugin for the configuration of the ICM base project.
 */
class ICMBaseProjectPlugin : Plugin<Project> {

    private lateinit var runtimeLibConfiguration: Configuration

    override fun apply(project: Project) {
        with(project) {
            if(project.rootProject == this) {
                logger.info("ICM base project plugin will be initialized")

                val extension = extensions.findByType(
                    IntershopExtension::class.java
                ) ?: extensions.create(
                    IntershopExtension.INTERSHOP_EXTENSION_NAME, IntershopExtension::class.java, this
                )

                addRuntimeDependencies(project, extension)


                tasks.maybeCreate("installRuntimeLib", Copy::class.java).apply {
                    from(runtimeLibConfiguration)

                    rename("(.*).dll", "ish-runtime.dll")
                    rename("(.*).so", "libish-runtime.so")
                    rename("(.*).dylib", "libish-runtime.dylib")
                    rename("(.*).setpgid", "setpgid")

                    into(File(project.buildDir, "runtime-lib"))
                }
            }
        }
    }

    private fun addRuntimeDependencies(project: Project, extension: IntershopExtension) {
        val dependencyHandler = project.dependencies

        runtimeLibConfiguration = project.configurations.maybeCreate("runtimeLib")
            .setTransitive(false)
            .setDescription("Configuration for native runtime library")
            .defaultDependencies {
                val dependencyBase = "${extension.baseConfig.runtimeModule}:${extension.baseConfig.runtimeVersion}"

                if(OsCheck.getDetectedOS() == OsCheck.OSType.MacOS) {
                    it.add( dependencyHandler.create("${dependencyBase}:darwin@dylib") )
                    it.add( dependencyHandler.create("${dependencyBase}:darwin@setpgid") )
                }
                if(OsCheck.getDetectedOS() == OsCheck.OSType.Linux) {
                    it.add( dependencyHandler.create("${dependencyBase}:linux@so") )
                    it.add( dependencyHandler.create("${dependencyBase}:linux@setpgid") )
                }
                if(OsCheck.getDetectedOS() == OsCheck.OSType.Windows) {
                    it.add( dependencyHandler.create("${dependencyBase}:win32@dll") )
                }
            }

        project.configurations.maybeCreate("dockerRuntimeLib")
            .setTransitive(false)
            .setDescription("Configuration for native runtime library used with Docker")
            .defaultDependencies {
                val dependencyBase = "${extension.baseConfig.runtimeModule}:${extension.baseConfig.runtimeVersion}"


                it.add( dependencyHandler.create("${dependencyBase}:darwin@dylib") )
                it.add( dependencyHandler.create("${dependencyBase}:darwin@setpgid") )
            }
    }
}
