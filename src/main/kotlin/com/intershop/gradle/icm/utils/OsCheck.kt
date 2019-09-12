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

package com.intershop.gradle.icm.utils

import java.util.Locale

/**
 * Utility class to identify the OS.
 */
object OsCheck {

    /**
     * Types of Operating Systems.
     */
    enum class OSType {
        Windows, MacOS, Linux, Other
    }

    private var detectedOS: OSType

    /**
     * Detect the operating system from the os.name System property and cache
     * the result.
     */
    init {
        val OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
        if (OS.indexOf("mac") >= 0 || OS.indexOf("darwin") >= 0) {
            detectedOS = OSType.MacOS
        } else if (OS.indexOf("win") >= 0) {
            detectedOS = OSType.Windows
        } else if (OS.indexOf("nux") >= 0) {
            detectedOS = OSType.Linux
        } else {
            detectedOS = OSType.Other
        }
    }

    /**
     * Provides the information for other classes.
     *
     * @returns - the operating system detected.
     */
    fun getDetectedOS() = detectedOS
}
