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

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Object to configure a simple file object to copy.
 */
open class FilePackage @Inject constructor(objectFactory: ObjectFactory ) {

    /**
     * This list contains includes for file list.
     *
     * @property includes list of includes
     */
    @get:Input
    var includes: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Add pattern to include.
     *
     * @param pattern Ant style pattern
     */
    fun include(pattern: String) {
        includes.add(pattern)
    }

    /**
     * Add a list of patterns to include.
     *
     * @param patterns Ant style pattern
     */
    fun includes(patterns: Collection<String>) {
        includes.addAll(patterns)
    }

    /**
     * This list contains excludes for file list.
     *
     * @property excludes list of includes
     */
    @get:Input
    var excludes: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Add pattern to include.
     *
     * @param pattern Ant style pattern
     */
    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    /**
     * Add a list of patterns to include.
     *
     * @param patterns Ant style pattern
     */
    fun excludes(patterns: Collection<String>) {
        excludes.addAll(patterns)
    }

    /**
     * The duplication strategy for this package.
     *
     * @property duplicateStrategy duplication strategy.
     */
    @get:Input
    val duplicateStrategy: Property<DuplicatesStrategy> = objectFactory.property(DuplicatesStrategy::class.java)

    @get:Optional
    @get:Input
    val targetPath: Property<String> = objectFactory.property(String::class.java)

    init {
        duplicateStrategy.set(DuplicatesStrategy.INHERIT)
    }
}

