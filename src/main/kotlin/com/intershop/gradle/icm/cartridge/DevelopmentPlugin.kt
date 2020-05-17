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
package com.intershop.gradle.icm.cartridge

import com.intershop.gradle.icm.utils.CartridgeStyle
import org.gradle.api.Project

/**
 * The project cartridge plugin applies all basic configurations
 * and tasks for a cartridge project, that can be provided as
 * module dependendcy to other projects.
 */
open class DevelopmentPlugin : AbstractCartridge() {

    override fun apply(project: Project) {
        with(project) {
            extensions.extraProperties.set("cartridge.style", CartridgeStyle.DEVELOPMENT.value)
            plugins.apply(ExternalPlugin::class.java)
            publishCartridge(project, CartridgeStyle.DEVELOPMENT.value)
        }
    }
}
