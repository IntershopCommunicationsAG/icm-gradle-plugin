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

package com.intershop.gradle.icm.tasks

import com.intershop.gradle.icm.utils.CartridgeUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * This is a helper task to list all dependencies of a BOM
 * file in a list.
 */
open class GetDependencyList @Inject constructor(objectFactory: ObjectFactory): DefaultTask() {

    @get:Input
    val dependency: Property<String> = objectFactory.property(String::class.java)

    /**
     * Main function of this task.
     */
    @TaskAction
    fun getlist() {
        val d = dependency.get().split(":")
        if(d.size < 3) {
            throw GradleException("The dependency is not complete! Only module dependencies are allowed")
        }

        val list = CartridgeUtil.getDepenendencySet(project, d[0], d[1], d[2])
        list.forEach {
            println("    $it")
        }
    }
}
