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
import com.intershop.gradle.icm.ICMProductPlugin.Companion.CONFIGURATION_DBINIT
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

/**
 * DBInit Gradle task 'dbinit'
 *
 * This task runs the ICM DB initialization for all available cartridges.
 */
open class DBInit : JavaExec() {

    companion object {
        const val DEFAULT_NAME = "dbinit"
    }

    init {
        group = "Intershop"
        description = "Runs the ICM DB initialization"

        jvmArgs("-showversion")

        val installRuntimeLib = project.tasks.getByName(ICMProductPlugin.TASK_INSTALLRUNTIMELIB)
        dependsOn(installRuntimeLib)
        systemProperty("java.library.path", "${installRuntimeLib.outputs.files.singleFile.absolutePath}")

        main = "com.intershop.tool.dbinit.DBInit"

        classpath = project.configurations.findByName(CONFIGURATION_DBINIT)

        minHeapSize = "300m"
        maxHeapSize = "2048m"

        systemProperty("java.system.class.loader","com.intershop.beehive.runtime.EnfinitySystemClassLoader")
        systemProperty("server.name","dbinit")
        systemProperty("intershop.classloader.add.dbinit","true")

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

        args("-classic")

        standardOutput = System.out
        errorOutput = System.err
    }

    @TaskAction
    override fun exec() {
        println("---Starting dbinit with gradle---")
        println("Commandline:")
        println(commandLine.toString())
        println("---                           ---")

        super.exec()
    }
}
