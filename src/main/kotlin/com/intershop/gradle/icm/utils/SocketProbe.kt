/*
 * Copyright 2020 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.intershop.gradle.icm.utils

import org.gradle.api.Project
import org.gradle.internal.service.ServiceRegistry
import java.net.Socket

class SocketProbe(
        private val project: Project,
        serviceRegistrySupplier : () -> ServiceRegistry,
        private val hostName : String,
        private val port : Int) : AbstractProbe(serviceRegistrySupplier) {

    companion object {
        fun toLocalhost(project: Project, serviceRegistrySupplier : () -> ServiceRegistry, port : Int) : SocketProbe {
            return SocketProbe(project, serviceRegistrySupplier, "localhost", port)
        }
    }

    override fun executeOnce(): Boolean {
        val reqDesc = describeRequest()
        try {
            Socket(hostName, port).use {
                project.logger.debug("Successfully probed {}", reqDesc)
            }
        } catch (e: Exception) {
            project.logger.debug("Unable to probe {}", reqDesc, e)
            return false
        }
        return true
    }

    override fun describeRequest(): String = "socket connection to $hostName:$port"

    override fun toString(): String {
        return "SocketProbe connecting to $hostName:$port retried each $retryInterval timing out " +
               "after $retryTimeout"
    }

}
