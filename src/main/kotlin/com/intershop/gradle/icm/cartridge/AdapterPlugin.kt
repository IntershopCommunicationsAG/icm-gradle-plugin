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
 * The adapter cartridge plugin applies all basic
 * configuration and tasks to a adapter cartridge project.
 */
open class AdapterPlugin : AbstractCartridge() {

    override fun apply(project: Project) {
        with(project) {
            extensions.extraProperties.set("cartridge.style", CartridgeStyle.ADAPTER.value)
            plugins.apply(ExternalPlugin::class.java)
            publishCartridge(project, CartridgeStyle.ADAPTER.value)
        }
    }
}
