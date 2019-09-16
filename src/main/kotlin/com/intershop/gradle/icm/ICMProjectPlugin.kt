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
import com.intershop.gradle.icm.tasks.WriteCartridgeClasspath
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import com.intershop.gradle.icm.utils.ICMPluginBase.CONFIGURATION_CARTRIDGE
import com.intershop.gradle.icm.utils.ICMPluginBase.CONFIGURATION_CARTRIDGERUNTIME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer

/**
 * The main plugin class of this plugin.
 */
class ICMProjectPlugin : Plugin<Project> {

    companion object {
        const val PROJECT_INFO_FILE = "version.properties"
        const val PROJECT_INFO_DIR = "serverInfoProps"

        const val CARTRIDGE_CLASSPATH_DIR = "classpath"
        const val CARTRIDGE_CLASSPATH_FILE = "cartridge.classpath"
    }

    override fun apply(project: Project) {
        with(project) {
            if(project.rootProject == this) {

                // base plugin must be applied
                if(plugins.findPlugin(ICMBasePlugin::class.java) == null) {
                    plugins.apply(ICMBasePlugin::class.java)
                }

                val extension = extensions.getByType(IntershopExtension::class.java)


                rootProject.subprojects.forEach { prj ->


                }
            } else {
                logger.warn("ICM build plugin will be not applied to the sub project '{}'", project.name)
            }
        }
    }
}
