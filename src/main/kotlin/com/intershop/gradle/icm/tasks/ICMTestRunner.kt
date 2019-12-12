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

import com.intershop.gradle.icm.ICMProductPlugin
import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

class ICMTestRunner : JavaExec() {

    companion object {
        const val DEFAULT_NAME = "testrunner"
    }

    private val outputDirProperty: DirectoryProperty = project.objects.directoryProperty()
    private val classnameProperty: Property<String> = project.objects.property(String::class.java)
    private val packagenameProperty: Property<String> = project.objects.property(String::class.java)
    private val suitenameProperty: Property<String> = project.objects.property(String::class.java)
    private val testhomeProperty: DirectoryProperty = project.objects.directoryProperty()
    
    init {
        group = "Intershop"
        description = "Start ICM Integration HTML Unit Tests"

        jvmArgs("-showversion")

        val installRuntimeLib = project.tasks.getByName(ICMProductPlugin.TASK_INSTALLRUNTIMELIB)
        dependsOn(installRuntimeLib)
        systemProperty("java.library.path", "${installRuntimeLib.outputs.files.singleFile.absolutePath}")

        main = "com.intershop.testrunner.IshTestrunner"

        classpath = project.configurations.findByName(ICMProductPlugin.CONFIGURATION_TESTRUNNER)

        maxHeapSize = "2048m"

        systemProperty("java.system.class.loader","com.intershop.beehive.runtime.EnfinitySystemClassLoader")
        systemProperty("server.name","etestrunner")
        systemProperty("intershop.classloader.add.tests" ,"true")

        val createServerInfoProperties = project.tasks.getByName(CreateServerInfoProperties.DEFAULT_NAME)
        dependsOn(createServerInfoProperties)
        systemProperty("intershop.VersionInfo", createServerInfoProperties.outputs.files.first().absolutePath)

        val createServerDirProperties = project.tasks.getByName(CreateServerDirProperties.DEFAULT_NAME)
        dependsOn(createServerDirProperties)
        systemProperty("intershop.ServerConfig", createServerDirProperties.outputs.files.first().absolutePath)

        var configDirectory : String? = System.getProperty("configDirectory")
        if(configDirectory == null && project.hasProperty("configDirectory")) {
            configDirectory = project.property("configDirectory").toString()
        }
        systemProperty("intershop.LocalConfig", "${configDirectory}/cluster.properties")

        val runtimejar = project.tasks.findByPath(":platform:runtime:jar")?.outputs?.files?.singleFile
        if(runtimejar != null) {
            systemProperty("javaagent", runtimejar)
        }
    }

    /**
     * The output file contains the results integration tests of ICM.
     *
     * @property outputFile real file on file system with descriptor
     */
    @get:OutputFile
    var outputDir: File
        get() = outputDirProperty.get().asFile
        set(value) = outputDirProperty.set(value)

    /**
     * Set provider for classname property.
     *
     * @param classname set provider for classname property
     */
    @Suppress( "unused")
    fun provideClassname(classname: Property<String>) {
        classnameProperty.set(classname)
    }

    /**
     * Test home path for test runner.
     *
     * @property testhome directory path
     */
    @get:Input
    var testhome: File?
        get()  {
            if(testhomeProperty.orNull != null) {
                return testhomeProperty.get().asFile
            } else {
                return null
            }
        }
        set(value) = testhomeProperty.set(value)

    @set:Option(option = "class", description = "class, which should be executed by the testrunner")
    @get:Optional
    @get:Input
    var classname: String?
        get() {
            if(classnameProperty.isPresent) {
                return classnameProperty.get()
            } else {
                return null
            }
        }
        set(value) = classnameProperty.set(value)

    /**
     * Set provider for packagename property.
     *
     * @param packagename set provider for packagename property
     */
    @Suppress( "unused")
    fun providePackagename(packagename: Property<String>) {
        packagenameProperty.set(packagename)
    }

    @set:Option(option = "package", description = "package, which should be executed by the testrunner")
    @get:Optional
    @get:Input
    var packagename: String?
        get() {
            if(packagenameProperty.isPresent) {
                return packagenameProperty.get()
            } else {
                return null
            }
        }
        set(value) = packagenameProperty.set(value)

    /**
     * Set provider for suitename property.
     *
     * @param suitename set provider for suitename property
     */
    @Suppress( "unused")
    fun provideSuitename(suitename: Property<String>) {
        suitenameProperty.set(suitename)
    }

    @set:Option(option = "suite", description = "test suite, which should be executed by the testrunner")
    @get:Optional
    @get:Input
    var suitename: String?
        get() {
            if(suitenameProperty.isPresent) {
                return suitenameProperty.get()
            } else {
                return null
            }
        }
        set(value) = suitenameProperty.set(value)


    @TaskAction
    override fun exec() {

        if(classname != null) {
            this.args("--class", classname)
        }
        if(packagename != null) {
            this.args("--package", packagename)
        }
        if(suitename != null) {
            this.args("--suite", suitename)
        }

        println("--- Starting testrunner with gradle ---")
        println("Commandline:")
        println(commandLine.toString())
        println("---                                 ---")

        super.exec()
    }
}
