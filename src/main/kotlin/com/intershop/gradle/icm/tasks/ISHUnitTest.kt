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

import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import java.io.File

/**
 * This class contains the base configuration for a special Intershop
 * test task. It runs special ishUnitTests.
 */
open class ISHUnitTest : Test() {

    init {
        group = "verification"
        description = "Runs the ish unit test in ICM"

        val installRuntimeLib = project.tasks.findByName("installRuntimeLib")
        if(installRuntimeLib != null) {
            systemProperties["java.library.path"] =
                "${installRuntimeLib.outputs.files.singleFile.absolutePath}"
        }

        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val mainSourceSet = javaConvention.sourceSets.getByName("main")

        testClassesDirs = mainSourceSet.output.classesDirs
        //setClasspath(mainSourceSet.runtimeClasspath + project.configurations.findByName("cartridgeRuntime"))

        include("tests/unit/**/*")
        include("tests/embedded/**/*")
        include("tests/query/**/*")

        exclude("tests/unit/com/intershop/ui/web/property/mapping/UIMapperTest.class")
        exclude("**/*TestSuite*")

        minHeapSize = "512m"
        maxHeapSize = "2048m"

        /*
jvmArgs '-agentlib:jdwp=transport=dt_socket,server=y,address=6666,suspend=n'
*/

        jvmArgs("-DUnitTestCase.allowServerRestart=false")
        jvmArgs("-DEmbeddedServerRule.loadAllCartridges=true")
        // jvmArgs("-Dintershop.ServerConfig=
        // ${rootProject.tasks.createServerDirProperties.outputs.files.first().absolutePath}")

        val cartridgeDescriptor = File(project.buildDir, "descriptor/cartridge.descriptor")
        jvmArgs("-DcartridgeDescriptor=${cartridgeDescriptor.absolutePath}")

        environment("INTERSHOP_EVENT_MESSENGERCLASS",
            "com.intershop.beehive.messaging.internal.noop.NoOpMessenger")
        environment("INTERSHOP_CACHESYNC_MESSENGERCLASS",
            "com.intershop.beehive.messaging.internal.noop.NoOpMessenger")
        environment("INTERSHOP_CACHEENGINE_WRAPPED_MESSENGERCLASS",
            "com.intershop.beehive.messaging.internal.noop.NoOpMessenger")
    }


}
