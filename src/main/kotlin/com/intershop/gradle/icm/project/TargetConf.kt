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

import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/**
 * An enumeration of target directories and files of special
 * environment typs - production, test and local server.
 */
enum class TargetConf(val value: String) {

    PRODUCTION("production") {
        override fun cartridges(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("container/cartridges")

        override fun libs(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("container/prjlibs")

        override fun config(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("container/config_folder")

        override fun cartridgelist(projectLayout: ProjectLayout): Provider<RegularFile> =
            projectLayout.buildDirectory.file("container/cartridgelist/cartridgelist.properties")
    },
    TEST("test") {
        override fun cartridges(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("testcontainer/cartridges")

        override fun libs(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("testcontainer/prjlibs")

        override fun config(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("testcontainer/config_folder")

        override fun cartridgelist(projectLayout: ProjectLayout): Provider<RegularFile> =
            projectLayout.buildDirectory.file("testcontainer/cartridgelist/cartridgelist.properties")
    },
    DEVELOPMENT("server") {
        override fun cartridges(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("server/cartridges")

        override fun libs(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("server/prjlibs")

        override fun config(projectLayout: ProjectLayout): Provider<Directory> =
            projectLayout.buildDirectory.dir("server/config_folder")

        override fun cartridgelist(projectLayout: ProjectLayout): Provider<RegularFile> =
            projectLayout.buildDirectory.file("server/cartridgelist/cartridgelist.properties")
    };

    /**
     * Returns the the provider of a directory
     * for external cartridgs.
     *
     * @return Provider<Directory>
     */
    abstract fun cartridges(projectLayout: ProjectLayout): Provider<Directory>

    /**
     * Returns the the provider of a directory
     * for external project libraries.
     *
     * @return Provider<Directory>
     */
    abstract fun libs(projectLayout: ProjectLayout): Provider<Directory>

    /**
     * Returns the the provider of a directory
     * for config folders.
     *
     * @return Provider<Directory>
     */
    abstract fun config(projectLayout: ProjectLayout): Provider<Directory>

    /**
     * Returns the the provider of a cartridge list file.
     *
     * @return Provider<RegularFile>
     */
    abstract fun cartridgelist(projectLayout: ProjectLayout): Provider<RegularFile>
}
