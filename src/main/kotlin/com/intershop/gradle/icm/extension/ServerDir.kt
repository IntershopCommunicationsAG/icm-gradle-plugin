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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * This is part of the project extension and describes
 * a list of directory configuration with excludes and
 * includes. Furthermore it contains a property for the target.
 * The task that uses this configuration transforms this
 * to copyspec configuration.
 *
 * @constructor creates a configuration of a simple copyspec.
 */
open class ServerDir
    @Inject constructor(objectFactory: ObjectFactory,
                        defaultTarget: String,
                        defaultExcludes: List<String>,
                        defaultIncludes: List<String>) {

    @get:Optional
    @get:Nested
    val dirs: NamedDomainObjectContainer<DirConfig> = objectFactory.domainObjectContainer(DirConfig::class.java)

    @get:Optional
    @get:Input
    val excludes: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Adds a single pattern to the exclude list.
     *
     * @param pattern Ant style file pattern.
     */
    fun exclude(pattern: String) = excludes.add(pattern)

    /**
     * Adds a collection of patterns to the exclude list.
     *
     * @param patterns a collection of Ant style file pattern.
     */
    fun excludes(patterns: Collection<String>) = excludes.addAll(patterns)

    @get:Optional
    @get:Input
    val includes: SetProperty<String> = objectFactory.setProperty(String::class.java)

    /**
     * Adds a single pattern to the include list.
     *
     * @param pattern Ant style file pattern.
     */
    fun include(pattern: String) = includes.add(pattern)

    /**
     * Adds a collection of patterns to the include list.
     *
     * @param patterns a collection of Ant style file pattern.
     */
    fun includes(patterns: Collection<String>) = includes.addAll(patterns)

    @get:Optional
    @get:Input
    val target: Property<String> = objectFactory.property(String::class.java)

    init {
        target.convention(defaultTarget)
        if(defaultExcludes.isNotEmpty()) {
            excludes.convention(defaultExcludes)
        }
        if(defaultIncludes.isNotEmpty()) {
            excludes.convention(defaultExcludes)
        }
    }
}
