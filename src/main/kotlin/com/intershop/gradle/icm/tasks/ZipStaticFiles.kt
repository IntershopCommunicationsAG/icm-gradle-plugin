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
import org.gradle.api.tasks.bundling.Zip
import java.io.File

/**
 * Task to create a package of static
 * content of a cartridge.
 */
open class ZipStaticFiles: Zip() {

    init {
        archiveClassifier.set("staticfiles")
        duplicatesStrategy =  DuplicatesStrategy.EXCLUDE

        destinationDirectory.set(File(project.buildDir, "staticfiles"))

        this.from("staticfiles/cartridge")
    }
}
