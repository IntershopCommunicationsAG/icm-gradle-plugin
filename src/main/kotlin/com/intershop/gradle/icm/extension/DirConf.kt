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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import java.io.File
import javax.inject.Inject

/**
 * Object to configure a simple directory object to copy.
 */
abstract class DirConf: Package() {

    /**
     * Inject service of ProjectLayout (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val projectLayout: ProjectLayout

    private val dirProperty: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Directory with configuration or sites..
     *
     * @property dir
     */
    var dir: File?
        get() = if(dirProperty.orNull != null) dirProperty.get().asFile else null
        set(value) = dirProperty.set(value)

    fun dir(path: String) {
        dirProperty.set(projectLayout.projectDirectory.dir(path))
    }
}
