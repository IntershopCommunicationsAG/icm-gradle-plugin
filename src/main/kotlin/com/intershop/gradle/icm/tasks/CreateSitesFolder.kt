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

import com.intershop.gradle.icm.extension.BaseProjectConfiguration
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Task to create sites folder.
 */
open class CreateSitesFolder @Inject constructor(
    objectFactory: ObjectFactory,
    private var fsOps: FileSystemOperations): AbstractCreateFolder(objectFactory, fsOps) {

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
    }

    @get:Internal
    override val classifier = "sites"

    @TaskAction
    fun runFolderCreation() {
        super.startFolderCreation()
    }

    /**
     *
     */
    override fun addConfySpec(cs: CopySpec, prjConf: BaseProjectConfiguration) {
        if(prjConf.sitesCopySpec != null) {
            cs.with(prjConf.sitesCopySpec)
        }
    }

    override fun getCartridgeListProps(zipFile: File): File? {
        return null
    }
}
