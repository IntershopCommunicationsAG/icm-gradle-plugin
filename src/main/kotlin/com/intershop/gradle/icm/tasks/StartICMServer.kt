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

import com.intershop.gradle.icm.ICMProductPlugin
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Starts an ICM from Sources.
 */
open class StartICMServer: SpawnJavaProcess() {

    companion object {
        const val DEFAULT_NAME = "startServer"
    }

    private val serverNameProperty: Property<String> = project.objects.property(String::class.java)
    private val classLoaderProperty: Property<String> = project.objects.property(String::class.java)

    init {
        group = "Intershop"
        description = "Start an ICM server from project sources"

        serverNameProperty.convention("appserver")
        main = "com.intershop.beehive.startup.ServletEngineStartup"

        classLoaderProperty.convention("com.intershop.beehive.runtime.EnfinitySystemClassLoader")

        classpath = project.configurations.getByName(ICMProductPlugin.CONFIGURATION_ICMSERVER)

        readyString = "Server started"

        jvmArgs("-server", "-showversion", "-d64", "-XX:NewRatio=8")

        minHeapSize = "1024m"
        maxHeapSize = "2048m"

        val installRuntimeLib = project.tasks.getByName(ICMProductPlugin.TASK_INSTALLRUNTIMELIB)
        dependsOn(installRuntimeLib)
        systemProperty("java.library.path", "${installRuntimeLib.outputs.files.singleFile.absolutePath}")

        systemProperty("java.net.preferIPv4Stack", "true")
        systemProperty("java.awt.headless", "true")
        systemProperty("server.name", serverNameProperty.get())
        systemProperty("java.system.class.loader", classLoaderProperty.get())

        val createServerInfoProperties = project.tasks.getByName(CreateServerInfoProperties.DEFAULT_NAME)
        dependsOn(createServerInfoProperties)
        systemProperty("intershop.VersionInfo", createServerInfoProperties.outputs.files.first().absolutePath)

        val createServerDirProperties = project.tasks.getByName(CreateServerDirProperties.DEFAULT_NAME)
        dependsOn(createServerDirProperties)
        systemProperty("intershop.ServerConfig", createServerDirProperties.outputs.files.first().absolutePath)

        var configDirectory : String? = System.getProperty("configDirectory")
        if(configDirectory == null && project.hasProperty("configDirectory")) {
            configDirectory = project.property("configDirectory").toString()
        }
        systemProperty("intershop.LocalConfig", "${configDirectory}/cluster.properties")

        val runtimejar = project.tasks.findByPath(":platform:runtime:jar")?.outputs?.files?.singleFile
        if(runtimejar != null) {
            systemProperty("javaagent", runtimejar)
        }

        args("start")
    }

    @TaskAction
    fun startServer() {
        super.spawnProcess()
    }
}
