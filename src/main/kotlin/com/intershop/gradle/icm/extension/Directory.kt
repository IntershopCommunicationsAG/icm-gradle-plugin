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

import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class Directory {

    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val pathProperty: Property<String> = objectFactory.property(String::class.java)
    private val excludeProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val includeProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)

    @Suppress("unused")
    fun providePath(path: Provider<String>) = pathProperty.set(path)

    var path by pathProperty

    @Suppress("unused")
    fun provideIncludes(includes: Provider<Set<String>>) = includeProperty.set(includes)

    @get:Input
    var includes by includeProperty

    fun include(pattern: String) {
        includeProperty.add(pattern)
    }

    fun includes(patterns: Collection<String>) {
        includeProperty.addAll(patterns)
    }

    @Suppress("unused")
    fun provideExcludes(excludes: Provider<Set<String>>) = excludeProperty.set(excludes)

    @get:Input
    var excludes by excludeProperty

    fun exclude(pattern: String) {
        excludeProperty.add(pattern)
    }

    fun excludes(patterns: Collection<String>) {
        excludeProperty.addAll(patterns)
    }
}