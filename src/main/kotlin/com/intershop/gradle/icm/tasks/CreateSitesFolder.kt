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

package com.intershop.gradle.icm.tasks

import com.intershop.gradle.icm.extension.CartridgeProject
import com.intershop.gradle.icm.extension.IntershopExtension
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Task to create sites folder.
 */
open class CreateSitesFolder @Inject constructor(
    objectFactory: ObjectFactory,
    fsOps: FileSystemOperations): AbstractCreateFolder(objectFactory, fsOps) {

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
    }

    @get:Internal
    override val classifier = "sites"

    /**
     * Main function to run the task functionality.
     */
    @TaskAction
    fun runFolderCreation() {
        super.startFolderCreation()
    }

    /**
     * Add copy spec for an package file.
     *
     * @param cs        base copy spec
     * @param pkgCS     package copy spec
     * @param prjConf   configuration of a base project
     * @param file      package file it self
     */
    override fun addCopyConfSpec(cs: CopySpec, pkgCS: CopySpec, prjConf: CartridgeProject, file: File) {
        with(prjConf) {
            sitesPackage.excludes.forEach {
                pkgCS.exclude(it)
            }
            sitesPackage.includes.forEach {
                pkgCS.include(it)
            }
            if (! sitesPackage.targetPath.isNullOrEmpty()) {
                pkgCS.into(sitesPackage.targetPath!!)
            }
            if (sitesPackage.duplicateStrategy != DuplicatesStrategy.INHERIT) {
                pkgCS.duplicatesStrategy = sitesPackage.duplicateStrategy
            }
        }
    }
}
