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

import com.intershop.gradle.icm.utils.EnvironmentType
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * This is part of the project extension and describes
 * different directory configurations for different
 * environment types (prod, dev, test).
 *
 * @constructor creates a configuration of a set of ServerDirs.
 */
open class ProjectServerDirs @Inject constructor(objectFactory: ObjectFactory ) {

    val base: ServerDirSet = objectFactory.newInstance(ServerDirSet::class.java)

    /**
     * Configures a ServerDirSet from an action
     * to the base configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun base(action: Action<in ServerDirSet>) {
        action.execute(base)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the base configuration.
     *
     * @param c ServerDirSet closure
     */
    fun base(c: Closure<ServerDirSet>) {
        ConfigureUtil.configure(c, base)
    }

    val prod: ServerDirSet = objectFactory.newInstance(ServerDirSet::class.java)

    /**
     * Configures a ServerDirSet from an action
     * to the prod configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun prod(action: Action<in ServerDirSet>) {
        action.execute(prod)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the prod configuration.
     *
     * @param c ServerDirSet closure
     */
    fun prod(c: Closure<ServerDirSet>) {
        ConfigureUtil.configure(c, prod)
    }

    val test: ServerDirSet = objectFactory.newInstance(ServerDirSet::class.java)

    /**
     * Configures a ServerDirSet from an action
     * to the test configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun test(action: Action<in ServerDirSet>) {
        action.execute(test)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the test configuration.
     *
     * @param c ServerDirSet closure
     */
    fun test(c: Closure<ServerDirSet>) {
        ConfigureUtil.configure(c, test)
    }

    val dev: ServerDirSet = objectFactory.newInstance(ServerDirSet::class.java)

    /**
     * Configures a ServerDirSet from an action
     * to the development configuration.
     *
     * @param action ServerDirSet configuration
     */
    fun dev(action: Action<in ServerDirSet>) {
        action.execute(dev)
    }

    /**
     * Configures a ServerDirSet from an action
     * to the development configuration.
     *
     * @param c ServerDirSet closure
     */
    fun dev(c: Closure<ServerDirSet>) {
        ConfigureUtil.configure(c, dev)
    }

    /**
     * Get serverdir set of environment type.
     *
     * @param type environment type
     */
    fun getServerDirSet(type: EnvironmentType): ServerDirSet {
        return when (type) {
            EnvironmentType.PRODUCTION    -> prod
            EnvironmentType.DEVELOPMENT   -> dev
            EnvironmentType.TEST          -> test
            else -> throw GradleException("Server dir configuration for $type is not available!")
        }
    }
}
