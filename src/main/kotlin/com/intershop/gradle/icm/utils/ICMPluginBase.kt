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

object ICMPluginBase {

    val CONFIGURATION_CARTRIDGE = "cartridge"
    val CONFIGURATION_CARTRIDGERUNTIME = "cartridgeRuntime"

    // ICM base project
    // ... configurations
    val CONFIGURATION_DBINIT = "dbinit"
    val CONFIGURATION_ICMSERVER = "icmserver"
    val CONFIGURATION_RUNTIME_LIB = "runtimeLib"
    val CONFIGURATION_DOCKER_RUNTIME_LIB = "dockerRuntimeLib"

    // ... parameters
    val PARAMETER_CONFIGDIR = "configDirectory"

    // ... tasks
    val TASK_INSTALLRUNTIMELIB = "installRuntimeLib"
    val TASK_INSTALLPROJECTCONFIG = "installProjectConfig"


}