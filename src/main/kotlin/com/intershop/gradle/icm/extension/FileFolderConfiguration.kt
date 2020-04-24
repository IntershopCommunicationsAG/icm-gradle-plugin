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

abstract class FileFolderConfiguration {

    @get:Inject
    abstract val objectFactory: ObjectFactory

    val base = objectFactory.newInstance(FileFolderSet::class.java)

    fun base(action: Action<in FileFolderSet>) {
        action.execute(base)
    }

    fun base(c: Closure<FileFolderSet>) {
        ConfigureUtil.configure(c, base)
    }

    val prod= objectFactory.newInstance(FileFolderSet::class.java)

    fun prod(action: Action<in FileFolderSet>) {
        action.execute(prod)
    }

    fun prod(c: Closure<FileFolderSet>) {
        ConfigureUtil.configure(c, prod)
    }

    val test = objectFactory.newInstance(FileFolderSet::class.java)

    fun test(action: Action<in FileFolderSet>) {
        action.execute(test)
    }

    fun test(c: Closure<FileFolderSet>) {
        ConfigureUtil.configure(c, test)
    }

    val dev = objectFactory.newInstance(FileFolderSet::class.java)

    fun dev(action: Action<in FileFolderSet>) {
        action.execute(dev)
    }

    fun dev(c: Closure<FileFolderSet>) {
        ConfigureUtil.configure(c, dev)
    }
}