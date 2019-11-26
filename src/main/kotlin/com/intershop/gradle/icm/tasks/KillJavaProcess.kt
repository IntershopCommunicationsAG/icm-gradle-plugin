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

import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Generic task for killing a process.
 */
open class KillJavaProcess: DefaultTask() {

    private val pidFileProperty: RegularFileProperty = project.objects.fileProperty()
    private val logOutputFileProperty: RegularFileProperty = project.objects.fileProperty()
    private val timeoutProperty: Property<Int> = project.objects.property(Int::class.java)

    init {
        pidFileProperty.convention { File(project.buildDir, "javaprocess/pid/process.pid") }
        logOutputFileProperty.convention { File(project.buildDir, "javaprocess/log/output.log")  }
        timeoutProperty.convention( 300 )
    }

    /**
     * The file will contain the pid of the process.
     *
     * @property pidFile real file on file system with pid
     */
    @get:InputFile
    var pidFile: File
        get() = pidFileProperty.get().asFile
        set(value) = pidFileProperty.set(value)

    /**
     * The file will contain the log output of the process.
     *
     * @property logOutputFile System out of the started process
     */
    @get:OutputFile
    var logOutputFile: File
        get() = logOutputFileProperty.get().asFile
        set(value) = logOutputFileProperty.set(value)

    /**
     * Set provider for timeout property.
     *
     * @param timeout maximum time in seconds before the task will be finished with an exception.
     */
    @Suppress("unused")
    fun providerTimeout(timeout: Provider<Int>) =
        timeoutProperty.set(timeout)

    @get:Input
    var timeout by timeoutProperty

    /**
     * Task action starts the java process in the background.
     */
    @TaskAction
    fun killProcess() {
        var pidFile = pidFileProperty.get().asFile

        if(! pidFile.exists()) {
            logger.warn("The java process does not exists.")
        } else {

            val endTime = System.currentTimeMillis() + (timeoutProperty.get().toLong() * 1000L)

            val pid = pidFile.readText().replace("\\s".toRegex(), "")

            val processBuilder = if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
                ProcessBuilder("taskkill " + pid)
            } else {
                ProcessBuilder("kill -9 " + pid)
            }

            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            val stdout = process.getInputStream()
            val reader = BufferedReader(InputStreamReader(stdout))

            var line: String?
            do {
                logger.info("Waiting for new line ....")
                line = reader.readLine()
                if (line != null) {
                    println(line)
                    logOutputFileProperty.get().asFile.printWriter().use { out -> out.println(line) }
                }

                try {
                    process.exitValue()
                    break
                } catch(ex: IllegalThreadStateException) {
                    project.logger.debug("Process for pid {} is still active.", pid)
                }
                if(System.currentTimeMillis() > endTime) {
                    project.logger.error("Process was not started in the expected time {}", timeoutProperty.get().toLong())
                    break
                }

            } while (true)

            try {
                process.exitValue()
            } catch (ex: IllegalThreadStateException) {
                project.logger.debug("Process for pid {} is still active.", pid)
                process.destroy()
                throw GradleException("Subprocesses of process with pid {} forcibly terminated.")
            }

            if(process.exitValue() != 0) {
                throw GradleException("Process with pid {} did not complete successfully.")
            }
        }
    }
}