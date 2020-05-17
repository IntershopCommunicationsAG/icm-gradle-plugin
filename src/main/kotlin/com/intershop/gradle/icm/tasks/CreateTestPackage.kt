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

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import java.io.File

/**
 * Task for the creation of a tar package. This
 * is used by the docker creation task.
 * This package contains test artifacts.
 */
open class CreateTestPackage: Tar() {

    companion object {
        const val DEFAULT_NAME = "createTestPkg"
    }

    init {
        group = "ICM server build"
        description = "Create the package of test artifacts for the Docker build"

        archiveBaseName.set("testpkg")
        archiveVersion.set("")

        compression = Compression.GZIP
        duplicatesStrategy =  DuplicatesStrategy.EXCLUDE

        destinationDirectory.set(File(project.buildDir, "packages"))
    }
}
