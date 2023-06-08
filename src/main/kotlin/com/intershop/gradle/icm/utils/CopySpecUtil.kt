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

import com.intershop.gradle.icm.extension.DirConfig
import com.intershop.gradle.icm.extension.ServerDir
import org.gradle.api.Project
import org.gradle.api.file.CopySpec

/**
 * This object provides methodes to transfer
 * configurations to a CopySpec.
 */
object CopySpecUtil {

    /**
     * Creates a CopySpec from a ServerDir configuration.
     *
     * @param project project
     * @param serverDir a ServerDir configurtion.
     */
    fun getCSForServerDir(project: Project, serverDir: ServerDir): CopySpec {
        val cs = project.copySpec()

        with(serverDir) {
            dirs.all { dirConfig ->
                cs.with(getCSForDirConfig(project, dirConfig))
            }

            if (excludes.get().isNotEmpty()) {
                cs.exclude(*excludes.get().toTypedArray())
            }

            if (includes.get().isNotEmpty()) {
                cs.exclude(*includes.get().toTypedArray())
            }

            if (target.isPresent && serverDir.target.get().isNotBlank()) {
                cs.into(target.get())
            }
        }
        return cs
    }

    private fun getCSForDirConfig(project: Project, dirConfig: DirConfig): CopySpec {
        val cs = project.copySpec()

        with(dirConfig) {
            if (dir.isPresent) {
                cs.from(dir.get())
            }
            if (excludes.get().isNotEmpty()) {
                cs.exclude(*excludes.get().toTypedArray())
            }
            if (includes.get().isNotEmpty()) {
                cs.exclude(*includes.get().toTypedArray())
            }
            if (target.isPresent && target.get().isNotBlank()) {
                cs.into(target.get())
            }
        }

        return cs
    }
}
