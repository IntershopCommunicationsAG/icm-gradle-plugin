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