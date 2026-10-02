/*
 * Copyright 2026 Intershop Communications AG.
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
import java.io.Serializable
import java.net.URI
import java.time.Duration

/**
 * Description of a [Probe] in plain values.
 *
 * A [Probe] is behaviour: it carries an HTTP client, a logger and the factory of the progress logger, none of which
 * can be stored by Gradle. A task therefore keeps the *description* of its probes - which is [Serializable], has
 * value semantics and is a valid task input - and turns it into a [Probe] with [createProbe] while it executes.
 */
sealed interface ProbeSpec : Serializable {

    /** Interval between two attempts. */
    val retryInterval: Duration

    /** Total time after which probing is given up. */
    val retryTimeout: Duration

    /**
     * Creates the described [Probe].
     *
     * The callbacks are parameters rather than properties of the description on purpose: they are behaviour, they
     * close over the state of the executing task and they are not [Serializable], so keeping them in the
     * description would make it unusable as a task input.
     *
     * @param logger                 logger of the executing task
     * @param progressLoggerFactory  progress logger factory injected into the executing task
     * @param onSuccess              called once after the probe succeeded, not per attempt
     * @param onFailure              called once after the probe gave up, not per attempt
     * @return the probe to execute
     */
    fun createProbe(
            logger: Logger,
            progressLoggerFactory: ProgressLoggerFactory,
            onSuccess: (Unit) -> Unit = { },
            onFailure: (Unit) -> Unit = { }): Probe
}

/**
 * Describes a [HttpProbe].
 *
 * @property target          the URI to request
 * @property retryInterval   interval between two attempts
 * @property retryTimeout    total time after which probing is given up
 * @property requestTimeout  timeout of a single HTTP request
 */
data class HttpProbeSpec(
        val target: URI,
        override val retryInterval: Duration,
        override val retryTimeout: Duration,
        val requestTimeout: Duration = Duration.ofSeconds(30)) : ProbeSpec {

    override fun createProbe(
            logger: Logger,
            progressLoggerFactory: ProgressLoggerFactory,
            onSuccess: (Unit) -> Unit,
            onFailure: (Unit) -> Unit): Probe =
            HttpProbe(logger, progressLoggerFactory, target, requestTimeout)
                    .withRetryInterval(retryInterval)
                    .withRetryTimeout(retryTimeout)
                    .onSuccess(onSuccess)
                    .onFailure(onFailure)
}

/**
 * Describes a [SocketProbe].
 *
 * @property hostName       the host to connect to
 * @property port           the port to connect to
 * @property retryInterval  interval between two attempts
 * @property retryTimeout   total time after which probing is given up
 */
data class SocketProbeSpec(
        val hostName: String,
        val port: Int,
        override val retryInterval: Duration,
        override val retryTimeout: Duration) : ProbeSpec {

    companion object {
        /**
         * Describes a [SocketProbe] connecting to `localhost`.
         */
        fun toLocalhost(port: Int, retryInterval: Duration, retryTimeout: Duration): SocketProbeSpec =
                SocketProbeSpec("localhost", port, retryInterval, retryTimeout)
    }

    override fun createProbe(
            logger: Logger,
            progressLoggerFactory: ProgressLoggerFactory,
            onSuccess: (Unit) -> Unit,
            onFailure: (Unit) -> Unit): Probe =
            SocketProbe(logger, progressLoggerFactory, hostName, port)
                    .withRetryInterval(retryInterval)
                    .withRetryTimeout(retryTimeout)
                    .onSuccess(onSuccess)
                    .onFailure(onFailure)
}
