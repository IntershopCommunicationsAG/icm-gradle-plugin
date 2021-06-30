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
package com.intershop.gradle.icm.project

/**
 * An enumeration of task names for the setup of special
 * environment types - production, test and local server.
 */
enum class TaskName(val value: String) {

    PRODUCTION("production") {
        override fun config() = "createConfigProd"
        override fun cartridgelist() = "extendCartridgeListProd"
        override fun cartridges() = "setupCartridgesProd"
        override fun prepare() = "prepareContainer"
    },
    TEST("test") {
        override fun config() = "createConfigTest"
        override fun cartridgelist() = "extendCartridgeListTest"
        override fun cartridges() = "setupCartridgesTest"
        override fun prepare() = "prepareTestContainer"
    },
    DEVELOPMENT("development") {
        override fun config() = "createConfig"
        override fun cartridgelist() = "extendCartridgeList"
        override fun cartridges() = "setupCartridges"
        override fun prepare() = "prepareServer"
    };

    /**
     * Returns the task name for the creation of
     * the necessary configuration folder.
     *
     * @return String
     */
    abstract fun config(): String

    /**
     * Returns the task name for the creation of
     * the cartridge list file.
     *
     * @return String
     */
    abstract fun cartridgelist(): String

    /**
     * Returns the task name for the creation of
     * a directory with external cartridges.
     *
     * @return String
     */
    abstract fun cartridges(): String

    /**
     * Returns the task name for the preparation
     * of all parts of server dir, this task calls
     * all others above.
     *
     * @return String
     */
    abstract fun prepare(): String
}
