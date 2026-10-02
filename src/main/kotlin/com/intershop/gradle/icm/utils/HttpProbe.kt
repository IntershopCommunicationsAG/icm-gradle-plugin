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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Probes an HTTP endpoint until it answers with an accepted status code.
 *
 * Takes the [Logger] and the [ProgressLoggerFactory] of the owning task instead of its `Project`, so that executing
 * this probe never touches the build model - see [AbstractProbe].
 */
class HttpProbe(
        private val logger : Logger,
        progressLoggerFactory : ProgressLoggerFactory,
        target : URI,
        requestTimeout : Duration = Duration.ofSeconds(30)) : AbstractProbe(progressLoggerFactory) {
    private val client : HttpClient = HttpClient.newHttpClient()
    private var statusCheck : (statusCode : Int) -> Boolean = { statusCode -> statusCode == 200 }
    val request : HttpRequest = HttpRequest.newBuilder().GET().timeout(requestTimeout).uri(target).build()

    override fun executeOnce(): Boolean {
        val reqDesc = describeRequest()
        val response = try {
            logger.debug("(Re-)trying to probe a {}", reqDesc)
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            logger.debug("Unable to probe {}", reqDesc, e)
            return false
        }
        logger.debug("Received response while probing a {}: status={}", reqDesc, response.statusCode())
        return statusCheck.invoke(response.statusCode())
    }

    override fun describeRequest(): String = "HTTP ${request.method()} to ${request.uri()}"

    fun requireHttpStatus(statusCheck : (statusCode : Int) -> Boolean ) : HttpProbe {
        this.statusCheck = statusCheck
        return this
    }

    override fun toString(): String {
        return "HttpProbe using ${request.method()} to '${request.uri()}' retried each $retryInterval timing out " +
               "after $retryTimeout with a request timeout of ${request.timeout()}"
    }

}
