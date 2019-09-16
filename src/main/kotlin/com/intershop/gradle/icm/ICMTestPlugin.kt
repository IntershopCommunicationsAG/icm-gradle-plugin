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

import com.intershop.gradle.icm.tasks.ISHUnitTest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin

/**
 * The plugin for Intershop ishUnitTests.
 */
class ICMTestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {

            // base plugin must be applied
            if(plugins.findPlugin(ICMBasePlugin::class.java) == null) {
                plugins.apply(ICMBasePlugin::class.java)
            }

            plugins.withType(JavaLibraryPlugin::class.java) {
                tasks.maybeCreate("ishUnitTest", ISHUnitTest::class.java)
            }
        }
    }
}
