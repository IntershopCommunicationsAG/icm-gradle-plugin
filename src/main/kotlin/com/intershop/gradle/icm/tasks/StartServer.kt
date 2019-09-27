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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import java.io.File

open class StartServer: DefaultTask() {

    private val serverName: Property<String> =
        project.objects.property(String::class.java)
    private val mainClass: Property<String> =
        project.objects.property(String::class.java)
    private val classLoader: Property<String> =
        project.objects.property(String::class.java)

    private val readString: Property<String> = project.objects.property(String::class.java)
    private val pidFile: RegularFileProperty = project.objects.fileProperty()

    private val systemProps: MapProperty<String, String> =
        project.objects.mapProperty(String::class.java, String::class.java)
    private val jvmArgs: ListProperty<String> =
        project.objects.listProperty(String::class.java)
    private val args: ListProperty<String> =
        project.objects.listProperty(String::class.java)

    private val processDir: DirectoryProperty = project.objects.directoryProperty()

    init {
        group = "Intershop"
        description = "Start an ICM server from project sources"

        serverName.convention("appserver")
        mainClass.convention("com.intershop.beehive.startup.ServletEngineStartup")
        classLoader.convention("com.intershop.beehive.runtime.EnfinitySystemClassLoader")

        readString.set("Server started")

        jvmArgs.convention(listOf("-server", "-showversion", "-d64", "-XX:NewRatio=8"))
        systemProps.convention(mapOf(
            "java.net.preferIPv4Stack" to "true",
            "java.awt.headless" to "true"))

        args.convention(listOf("start"))

        pidFile.set(File(project.buildDir, "serverrun/server.pid"))
    }

    @TaskAction
    fun startServer() {

    }

    fun buildProcess(directory: String): Process {
        val builder = ProcessBuilder("command")
        builder.redirectErrorStream(true)
        builder.directory(processDir.get().asFile)
        return builder.start()
    }

    fun extractPidFromProcess(process: Process): Int {
        val pidField = process.javaClass.getDeclaredField("pid")
        pidField.isAccessible = true
        return pidField.getInt(process)
    }
}