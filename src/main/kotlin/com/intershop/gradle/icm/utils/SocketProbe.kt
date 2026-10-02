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

import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import java.net.Socket

/**
 * Probes a TCP endpoint until a socket connection can be established.
 *
 * Takes the [Logger] and the [ProgressLoggerFactory] of the owning task instead of its `Project`, so that executing
 * this probe never touches the build model - see [AbstractProbe].
 */
class SocketProbe(
        private val logger: Logger,
        progressLoggerFactory : ProgressLoggerFactory,
        private val hostName : String,
        private val port : Int) : AbstractProbe(progressLoggerFactory) {

    companion object {
        fun toLocalhost(logger: Logger, progressLoggerFactory : ProgressLoggerFactory, port : Int) : SocketProbe {
            return SocketProbe(logger, progressLoggerFactory, "localhost", port)
        }
    }

    override fun executeOnce(): Boolean {
        val reqDesc = describeRequest()
        try {
            Socket(hostName, port).use {
                logger.debug("Successfully probed {}", reqDesc)
            }
        } catch (e: Exception) {
            logger.debug("Unable to probe {}", reqDesc, e)
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
