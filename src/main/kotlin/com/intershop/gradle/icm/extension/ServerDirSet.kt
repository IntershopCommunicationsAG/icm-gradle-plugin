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
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

abstract class ServerDirSet @Inject constructor(objectFactory: ObjectFactory ) {

    //@get:Inject
    //abstract val objectFactory: ObjectFactory

    val sites: ServerDir = objectFactory.newInstance(
        ServerDir::class.java, "sites",
        listOf<String>(),
        listOf<String>())

    fun sites(action: Action<in ServerDir>) {
        action.execute(sites)
    }

    fun sites(c: Closure<ServerDir>) {
        ConfigureUtil.configure(c, sites)
    }

    val config: ServerDir = objectFactory.newInstance(
        ServerDir::class.java,
        "system-conf",
        listOf("**/**/${CARTRIDGELIST_FILENAME}"),
        listOf<String>())

    fun config(action: Action<in ServerDir>) {
        action.execute(config)
    }

    fun config(c: Closure<ServerDir>) {
        ConfigureUtil.configure(c, config)
    }
}