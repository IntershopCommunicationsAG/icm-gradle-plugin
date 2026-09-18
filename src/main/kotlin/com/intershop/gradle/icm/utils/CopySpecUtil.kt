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
     * Applies a ServerDir configuration to an existing CopySpec.
     *
     * Use this overload at task execution time. In contrast to {@link #getCSForServerDir} it does not
     * need a project to create a CopySpec, so it can be used inside a
     * {@code FileSystemOperations.copy {}} block.
     *
     * @param parent    the CopySpec the configuration is added to as a child spec
     * @param serverDir a ServerDir configuration
     */
    fun applyServerDirTo(parent: CopySpec, serverDir: ServerDir) {
        with(serverDir) {
            val targetPath = if (target.isPresent && target.get().isNotBlank()) target.get() else ""

            parent.into(targetPath) { cs ->
                dirs.all { dirConfig ->
                    applyDirConfigTo(cs, dirConfig)
                }

                if (excludes.get().isNotEmpty()) {
                    cs.exclude(*excludes.get().toTypedArray())
                }

                // NOTE: keeps the behaviour of getCSForServerDir - includes are added as excludes there
                if (includes.get().isNotEmpty()) {
                    cs.exclude(*includes.get().toTypedArray())
                }
            }
        }
    }

    private fun applyDirConfigTo(parent: CopySpec, dirConfig: DirConfig) {
        with(dirConfig) {
            if (dir.isPresent) {
                val targetPath = if (target.isPresent && target.get().isNotBlank()) target.get() else ""

                parent.into(targetPath) { cs ->
                    cs.from(dir.get())

                    if (excludes.get().isNotEmpty()) {
                        cs.exclude(*excludes.get().toTypedArray())
                    }

                    // NOTE: keeps the behaviour of getCSForDirConfig - includes are added as excludes there
                    if (includes.get().isNotEmpty()) {
                        cs.exclude(*includes.get().toTypedArray())
                    }
                }
            }
        }
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
