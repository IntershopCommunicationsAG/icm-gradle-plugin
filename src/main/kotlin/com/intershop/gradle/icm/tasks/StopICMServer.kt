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
package com.intershop.gradle.icm.tasks

import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Stops an ICM server from sources by killing the process.
 * This can be replaced in the future by calling server stop (Java process)
 */
open class StopICMServer: KillJavaProcess() {

    companion object {
        const val DEFAULT_NAME = "stopServer"
    }

    init {
        pidFile = File(project.buildDir, "appserver/pid/process.pid")
        logOutputFile = File(project.buildDir, "appserver/log/output.log")
        timeout = 600
    }

    /**
     * This function represents the logic of this task.
     */
    @TaskAction
    fun stopServer() {
        super.killProcess()
    }
}