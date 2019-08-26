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
package com.intershop.gradle.icm.extension

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Extension for ICM properties.
 */
open class IntershopExtension(var project: Project)  {

    companion object {
        // names for the plugin
        const val INTERSHOP_EXTENSION_NAME = "intershop"
        const val INTERSHOP_GROUP_NAME = "Intershop Commerce Management build plugin"
    }

    val projectInfo: ProjectInfo = ProjectInfo(project)

    fun projectInfo(closure: Closure<Any>) {
        project.configure(projectInfo, closure)
    }

    fun projectInfo(action: Action<in ProjectInfo>) {
        action.execute(projectInfo)
    }
}
