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
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Object to configure a simple file object to copy.
 */
abstract class PackageConf {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val excludeProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)
    private val includeProperty: SetProperty<String> = objectFactory.setProperty(String::class.java)

    private val duplicateStrategyProperty: Property<DuplicatesStrategy>
            = objectFactory.property(DuplicatesStrategy::class.java)

    private val targetPathProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        duplicateStrategyProperty.set(DuplicatesStrategy.INHERIT)
    }

    /**
     * Set provider for includes matches.
     *
     * @param includes list of includes matches.
     */
    @Suppress("unused")
    fun provideIncludes(includes: Provider<Set<String>>) = includeProperty.set(includes)

    /**
     * This list contains includes for file list.
     *
     * @property includes list of includes
     */
    @get:Input
    var includes by includeProperty

    /**
     * Add pattern to include.
     *
     * @param pattern Ant style pattern
     */
    fun include(pattern: String) {
        includeProperty.add(pattern)
    }

    /**
     * Add a list of patterns to include.
     *
     * @param patterns Ant style pattern
     */
    fun includes(patterns: Collection<String>) {
        includeProperty.addAll(patterns)
    }

    /**
     * Set provider for excludes matches.
     *
     * @param excludes list of excludes matches.
     */
    @Suppress("unused")
    fun excludes(excludes: Provider<Set<String>>) = excludeProperty.set(excludes)

    /**
     * This list contains excludes for file list.
     *
     * @property excludes list of includes
     */
    @get:Input
    var excludes by excludeProperty

    /**
     * Add pattern to include.
     *
     * @param pattern Ant style pattern
     */
    fun exclude(pattern: String) {
        excludeProperty.add(pattern)
    }

    /**
     * Add a list of patterns to include.
     *
     * @param patterns Ant style pattern
     */
    fun excludes(patterns: Collection<String>) {
        excludeProperty.addAll(patterns)
    }

    /**
     * Set provider for duplication strategy.
     *
     * @param duplicateStrategy duplication strategy.
     */
    @Suppress("unused")
    fun provideDuplicateStrategy(duplicateStrategy: Provider<DuplicatesStrategy>)
            = duplicateStrategyProperty.set(duplicateStrategy)

    /**
     * The duplication strategy for this package.
     *
     * @property duplicateStrategy duplication strategy.
     */
    @get:Input
    var duplicateStrategy by duplicateStrategyProperty

    /**
     * Set provider for target path configuration.
     *
     * @param targetPath list of includes matches.
     */
    @Suppress("unused")
    fun provideTargetPath(targetPath: Provider<String>) = targetPathProperty.set(targetPath)

    /**
     * This list contains includes for file list.
     *
     * @property includes list of includes
     */
    @get:Optional
    @get:Input
    var targetPath: String?
        get() = targetPathProperty.orNull
        set(value) = targetPathProperty.set(value)
}
