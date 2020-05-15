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
import org.gradle.api.provider.Provider

/**
 * An enumeration for copy lib task configuration.
 */
enum class TaskConfCopyLib(val value: String) {

    PRODUCTION("production") {
        override fun description() = "Copy all 3rd party libs to one folder for a container."
        override fun taskname() = "copyLibsProd"
        override fun targetpath(projectLayout: ProjectLayout) = TargetConf.PRODUCTION.libs(projectLayout)
    },
    TEST("test") {
        override fun description() = "Copy all 3rd party libs to one folder for a test container."
        override fun taskname() = "copyLibsTest"
        override fun targetpath(projectLayout: ProjectLayout) = TargetConf.TEST.libs(projectLayout)
    },
    DEVELOPMENT("development") {
        override fun description() = "Copy all 3rd party libs to one folder for a local server."
        override fun taskname() = "copyLibs"
        override fun targetpath(projectLayout: ProjectLayout) = TargetConf.DEVELOPMENT.libs(projectLayout)
    };

    /**
     * Returns the description of the task.
     *
     * @return String
     */
    abstract fun description(): String

    /**
     * Returns the task name of the task.
     *
     * @return String
     */
    abstract fun taskname(): String

    /**
     * Returns the target directory provider.
     *
     * @return Provider<Directory>
     */
    abstract fun targetpath(projectLayout: ProjectLayout) : Provider<Directory>
}
