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

import com.intershop.gradle.icm.ICMBasePlugin
import com.intershop.gradle.icm.ICMProductPlugin.Companion.TASK_INSTALLRUNTIMELIB
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.ProjectReportsPluginConvention
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.testing.Test
import java.io.File

/**
 * This class contains the base configuration for a special Intershop
 * test task. It runs special ishUnitTests.
 */
open class ISHUnitTest : Test() {

    init {
        group = "verification"
        description = "Runs the ISH unit test (package: tests.unit) in ICM"

        val installRuntimeLib = project.tasks.getByName(TASK_INSTALLRUNTIMELIB)
        dependsOn(installRuntimeLib)
        systemProperty("java.library.path", "${installRuntimeLib.outputs.files.singleFile.absolutePath}")

        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val mainSourceSet = javaConvention.sourceSets.getByName(MAIN_SOURCE_SET_NAME)

        testClassesDirs = mainSourceSet.output.classesDirs
        setClasspath(mainSourceSet.runtimeClasspath +
                project.configurations.findByName(ICMBasePlugin.CONFIGURATION_CARTRIDGERUNTIME))

        include("tests/unit/**/*")
        include("tests/embedded/**/*")
        include("tests/query/**/*")

        exclude("tests/unit/com/intershop/ui/web/property/mapping/UIMapperTest.class")
        exclude("**/*TestSuite*")

        minHeapSize = "512m"
        maxHeapSize = "2048m"

        systemProperty("UnitTestCase.allowServerRestart", "false")
        systemProperty("EmbeddedServerRule.loadAllCartridges","true")

        val createServerInfoProperties = project.tasks.getByName(CreateServerInfoProperties.DEFAULT_NAME)
        dependsOn(createServerInfoProperties)
        systemProperty("intershop.VersionInfo", createServerInfoProperties.outputs.files.first().absolutePath)

        val createServerDirProperties = project.tasks.getByName(CreateServerDirProperties.DEFAULT_NAME)
        dependsOn(createServerDirProperties)
        systemProperty("intershop.ServerConfig=", createServerDirProperties.outputs.files.first().absolutePath)

        var configDirectory : String? = System.getProperty("configDirectory")
        if(configDirectory == null && project.hasProperty("configDirectory")) {
            configDirectory = project.property("configDirectory").toString()
        }
        systemProperty("intershop.LocalConfig", "${configDirectory}/cluster.properties")

        val writeCartridgeDescriptor = project.tasks.getByName(WriteCartridgeDescriptor.DEFAULT_NAME)
        dependsOn(writeCartridgeDescriptor)
        systemProperty("cartridgeDescriptor", writeCartridgeDescriptor.outputs.files.first().absolutePath)

        environment("INTERSHOP_EVENT_MESSENGERCLASS",
            "com.intershop.beehive.messaging.internal.noop.NoOpMessenger")
        environment("INTERSHOP_CACHESYNC_MESSENGERCLASS",
            "com.intershop.beehive.messaging.internal.noop.NoOpMessenger")
        environment("INTERSHOP_CACHEENGINE_WRAPPED_MESSENGERCLASS",
            "com.intershop.beehive.messaging.internal.noop.NoOpMessenger")

        val reportConvention = project.convention.getPlugin(ProjectReportsPluginConvention::class.java)
        reports.html.destination = File(reportConvention.projectReportDir, "ishUnitTest")

        reports.junitXml.destination = File(project.buildDir, "test-results/ishUnitTest")
    }

}
