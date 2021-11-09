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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Predicate

class HttpProbe(target : URI) {
    private val logger: Logger = LoggerFactory.getLogger(HttpProbe::class.java)
    private val client : HttpClient = HttpClient.newHttpClient()
    private var retryInterval = Duration.ofMinutes(1)
    private var retryTimeout = retryInterval.multipliedBy(3)
    private var statusCheck = Predicate<Int> { statusCode -> statusCode == 200 }
    val request : HttpRequest = HttpRequest.newBuilder().GET().uri(target).build()

    fun execute(): Boolean {
        val start = Instant.now()
        var success = executeOnce()
        while (!success){
            // sleep for <retryInterval>
            Thread.sleep(retryInterval.toMillis())
            // calculate if <retryTimeout> is exceeded
            val end = Instant.now()
            val dur = Duration.ofMillis(start.until(end, ChronoUnit.MILLIS))
            if (dur > retryTimeout){
                logger.warn("Failed to probe a HTTP {} to {} (total retry duration is {})", request.method(),
                        request.uri(), dur)
                // if so fail
                return false
            }
            success = executeOnce()
        }
        val totalDuration = Duration.ofMillis(start.until(Instant.now(), ChronoUnit.MILLIS))
        logger.info("Successfully probed a HTTP {} to {} (total duration is {})", request.method(), request.uri(),
                totalDuration)
        return true
    }

    private fun executeOnce(): Boolean {
        logger.debug("Retrying to probe a HTTP {} to {}", request.method(), request.uri())
        val response = try {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            logger.debug("Unable to probe a HTTP {} to {}", request.method(), request.uri(), e)
            return false
        }
        logger.debug("Received response while probing a HTTP {} to {}: status={}", request.method(), request.uri(),
                response.statusCode())
        return statusCheck.test(response.statusCode())
    }

    fun withRetryInterval(interval : Duration) : HttpProbe {
        retryInterval = interval
        return this
    }

    fun withRetryTimeout(timeout : Duration) : HttpProbe {
        retryTimeout = timeout
        return this
    }

    fun requireHttpStatus(statusCheck : Predicate<Int> ) : HttpProbe {
        this.statusCheck = statusCheck
        return this
    }

}
