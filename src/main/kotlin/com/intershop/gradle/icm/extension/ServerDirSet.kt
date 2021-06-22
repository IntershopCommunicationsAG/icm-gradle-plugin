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

import com.intershop.gradle.icm.ICMProjectPlugin.Companion.CARTRIDGELIST_FILENAME
import com.intershop.gradle.icm.tasks.CreateServerInfo.Companion.VERSIONINFO_FILENAME
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * This is part of the project extension and describes a set
 * of directory configurations for configuration folders.
 * The task that uses this configuration transforms this
 * to copyspec configuration.
 *
 * @constructor creates a configuration of a simple copyspec.
 */
open class ServerDirSet @Inject constructor(val project: Project, objectFactory: ObjectFactory ) {

    val config: ServerDir = objectFactory.newInstance(
        ServerDir::class.java,
        "system-conf",
        listOf("**/cluster/${CARTRIDGELIST_FILENAME}", "**/cluster/${VERSIONINFO_FILENAME}"),
        listOf<String>())

    /**
     * Configures a directory configuration
     * of the configuration server folder.
     *
     * @parm action Action to configures a ServerDir
     */
    fun config(action: Action<in ServerDir>) {
        action.execute(config)
    }

    /**
     * Configures a directory configuration
     * of the configuration server folder.
     *
     * @parm action Closure to configures a ServerDir
     */
    fun config(c: Closure<ServerDir>) {
        project.configure(config, c)
    }
}
