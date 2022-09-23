/*
 * Copyright 2022 Intershop Communications AG.
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

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object DependencyListUtil {

    fun getIDList(envType: String, listFile: RegularFile): List<String> {
        val list = mutableListOf<String>()
        try {
            BufferedReader(FileReader(listFile.asFile)).use { br ->
                br.lines().forEach { if (it.isNotEmpty()) list.add(it) }
            }
        } catch (e: IOException) {
            throw GradleException("It was not possible to read ${listFile.asFile} for ${envType}")
        }
        return list.sorted()
    }
}
