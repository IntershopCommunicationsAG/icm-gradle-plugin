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

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.service.ServiceRegistry
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

abstract class AbstractProbe(private val serviceRegistrySupplier : () -> ServiceRegistry) : Probe {
    var retryInterval = Duration.ofMinutes(1)
    var retryTimeout = retryInterval.multipliedBy(3)
    private var onSuccess : (Unit) -> Unit = { }
    private var onFailure : (Unit) -> Unit = { }

    override fun execute(): Boolean {
        // create progressLogger for pretty printing of terminal log progression.
        val reqDesc = describeRequest()
        val progressLogger = getProgressLogger().start(
                "Probing a $reqDesc expecting to become available and to response properly.", "started")

        val start = Instant.now()
        var success = executeOnce()
        var tryCount = 1
        while (!success){
            // sleep for <retryInterval>
            Thread.sleep(retryInterval.toMillis())
            // calculate if <retryTimeout> is exceeded
            val end = Instant.now()
            val dur = Duration.ofMillis(start.until(end, ChronoUnit.MILLIS))
            val count = if(tryCount>1){ "$tryCount times" }else{ "once" }
            progressLogger.progress("${toString()} executed $count, elapsed duration: $dur, timeout: $retryTimeout")
            if (dur > retryTimeout){
                // if so fail
                progressLogger.completed("Failed to probe a $reqDesc (total retry duration is ${dur})", true)
                onFailure.invoke(Unit)
                return false
            }
            success = executeOnce()
            tryCount++
        }
        val totalDuration = Duration.ofMillis(start.until(Instant.now(), ChronoUnit.MILLIS))
        progressLogger.completed(
                "Failed to probe a ${describeRequest()} (total retry duration is ${totalDuration})",
                false
        )
        onSuccess.invoke(Unit)
        return true
    }

    /**
     * Executes this probe once return `true` on success and `false` otherwise
     */
    protected abstract fun executeOnce(): Boolean

    /**
     * Generates a user readable description of a request
     */
    protected abstract fun describeRequest() : String

    override fun withRetryInterval(interval : Duration) : Probe {
        retryInterval = interval
        return this
    }

    override fun withRetryTimeout(timeout : Duration) : Probe {
        retryTimeout = timeout
        return this
    }

    override fun onSuccess(onSuccess : (Unit) -> Unit) : Probe {
        this.onSuccess = onSuccess
        return this
    }

    override fun onFailure(onFailure : (Unit) -> Unit) : Probe {
        this.onFailure = onFailure
        return this
    }

    private fun getProgressLogger() : ProgressLogger {
        val factory = serviceRegistrySupplier.invoke().get(ProgressLoggerFactory::class.java)
        return factory.newOperation(javaClass)
    }

}
