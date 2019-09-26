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

import com.intershop.gradle.icm.ICMProductPlugin.Companion.TASK_ISHUNIT_PARALLEL
import com.intershop.gradle.icm.ICMProductPlugin.Companion.TASK_ISHUNIT_SERIAL
import com.intershop.gradle.icm.tasks.DBInit
import com.intershop.gradle.icm.tasks.ISHUnitTest
import com.intershop.gradle.icm.tasks.WriteCartridgeDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin

/**
 * The plugin for Intershop ishUnitTests.
 */
class ICMTestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            plugins.withType(JavaLibraryPlugin::class.java) {
                val taskName = "ishUnitTest"
                val isSerialTest = project.hasProperty("serialISHUnitTest")
                        && project.property("serialISHUnitTest").toString().toLowerCase() == "true"

                if (!ICMBasePlugin.checkForTask(tasks, taskName)) {
                    val mainTestTask = if(isSerialTest) { TASK_ISHUNIT_SERIAL } else { TASK_ISHUNIT_PARALLEL }

                    val parallelTask = rootProject.tasks.getByName(TASK_ISHUNIT_PARALLEL)
                    val dbinitTask = project.tasks.getByName(DBInit.DEFAULT_NAME)
                    val ishUnitMain = rootProject.tasks.getByName(mainTestTask)

                    val task = tasks.register(taskName, ISHUnitTest::class.java) {
                        it.dependsOn(dbinitTask)
                        if(isSerialTest) {
                            it.mustRunAfter(parallelTask)
                        }
                    }
                    ishUnitMain.dependsOn(task)
                }

            }
        }
    }
}
