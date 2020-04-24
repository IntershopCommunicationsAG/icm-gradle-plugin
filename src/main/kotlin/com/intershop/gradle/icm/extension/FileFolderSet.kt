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
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

abstract class FileFolderSet {

    @get:Inject
    abstract val objectFactory: ObjectFactory

    val sites = objectFactory.newInstance(FileFolder::class.java)

    fun sites(action: Action<in FileFolder>) {
        action.execute(sites)
    }

    fun sites(c: Closure<FileFolder>) {
        ConfigureUtil.configure(c, sites)
    }

    val config = objectFactory.newInstance(FileFolder::class.java)

    fun config(action: Action<in FileFolder>) {
        action.execute(config)
    }

    fun config(c: Closure<FileFolder>) {
        ConfigureUtil.configure(c, config)
    }
}