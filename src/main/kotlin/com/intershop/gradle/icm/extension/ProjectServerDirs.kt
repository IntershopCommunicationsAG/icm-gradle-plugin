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

import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.icm.utils.EnvironmentType
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * This is part of the project extension and describes
 * different directory configurations for different
 * environment types (prod, dev, test).
 *
 * @constructor creates a configuration of a set of ServerDirs.
 */
@Deprecated("Configuration via folder is unsupported since 5.6.0", level = DeprecationLevel.WARNING)
open class ProjectServerDirs @Inject constructor(val project: Project, objectFactory: ObjectFactory ) {

    val base: ServerDir = objectFactory.newInstance(
        ServerDir::class.java,
        "system-conf",
        listOf("**/cluster/${CreateServerInfo.VERSIONINFO_FILENAME}"),
        listOf<String>())

    /**
     * Configures a ServerDirSet from an action
     * to the base configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun base(action: Action<in ServerDir>) {
        action.execute(base)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the base configuration.
     *
     * @param c ServerDirSet closure
     */
    fun base(c: Closure<ServerDir>) {
        project.configure(base, c)
    }

    val prod: ServerDir = objectFactory.newInstance(
        ServerDir::class.java,
        "system-conf",
        listOf("**/cluster/${CreateServerInfo.VERSIONINFO_FILENAME}"),
        listOf<String>())

    /**
     * Configures a ServerDirSet from an action
     * to the prod configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun prod(action: Action<in ServerDir>) {
        action.execute(prod)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the prod configuration.
     *
     * @param c ServerDirSet closure
     */
    fun prod(c: Closure<ServerDir>) {
        project.configure(prod, c)
    }

    val test: ServerDir = objectFactory.newInstance(
        ServerDir::class.java,
        "system-conf",
        listOf("**/cluster/${CreateServerInfo.VERSIONINFO_FILENAME}"),
        listOf<String>())

    /**
     * Configures a ServerDirSet from an action
     * to the test configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun test(action: Action<in ServerDir>) {
        action.execute(test)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the test configuration.
     *
     * @param c ServerDirSet closure
     */
    fun test(c: Closure<ServerDir>) {
        project.configure(test, c)
    }

    val dev: ServerDir = objectFactory.newInstance(
        ServerDir::class.java,
        "system-conf",
        listOf("**/cluster/${CreateServerInfo.VERSIONINFO_FILENAME}"),
        listOf<String>())

    /**
     * Configures a ServerDirSet from an action
     * to the development configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun dev(action: Action<in ServerDir>) {
        action.execute(dev)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the development configuration.
     *
     * @param c ServerDirSet closure
     */
    fun dev(c: Closure<ServerDir>) {
        project.configure(dev, c)
    }

    /**
     * Get serverdir set of environment type.
     *
     * @param type environment type
     */
    fun getServerDir(type: EnvironmentType): ServerDir {
        return when (type) {
            EnvironmentType.PRODUCTION    -> prod
            EnvironmentType.DEVELOPMENT   -> dev
            EnvironmentType.TEST          -> test
            else -> throw GradleException("Server dir configuration for $type is not available!")
        }
    }
}
