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

import java.time.Duration

/**
 * Defines a probe: execute a certain action in a defines _retry interval_ until it is a success or the _retry timeout_
 * is reached.
 */
interface Probe {
    /**
     * Executes this probe
     * @return `true` on success otherwise `false`
     */
    fun execute(): Boolean

    /**
     * Sets the _retry interval_
     * @return this [Probe]
     */
    fun withRetryInterval(interval : Duration) : Probe

    /**
     * Sets the _retry timeout_
     * @return this [Probe]
     */
    fun withRetryTimeout(timeout : Duration) : Probe

    /**
     * Assigns a callback that is executed when this [Probe] finishes with a success. The callback is not called for
     * each internal retry attempt.
     */
    fun onSuccess(onSuccess : (Unit) -> Unit) : Probe

    /**
     * Assigns a callback that is executed when this [Probe] finishes with a failure. The callback is not called for
     * each internal retry attempt.
     */
    fun onFailure(onFailure : (Unit) -> Unit) : Probe
}
