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

import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * CreateServerDirProperties Gradle task 'createServerDirProperties'
 *
 * This task creates a configuration file with all available project
 * directories of the ICM server.
 */
open class CreateServerDirProperties : DefaultTask() {

    private val outputFileProperty: RegularFileProperty = project.objects.fileProperty()
    private val configDirProperty: DirectoryProperty = project.objects.directoryProperty()
    private val sitesDirProperty: DirectoryProperty = project.objects.directoryProperty()
    private val sourceListPropery: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val cleanDirProperty: Property<Boolean> = project.objects.property(Boolean::class.java)

    private val shareReportingDirProperty: DirectoryProperty = project.objects.directoryProperty()
    private val shareDistDirProperty: DirectoryProperty = project.objects.directoryProperty()
    private val shareImpexDirProperty: DirectoryProperty = project.objects.directoryProperty()

    companion object {
        const val DEFAULT_NAME = "createServerDirProperties"
        const val SERVER_DIRECTORY_PROPERTIES_DIR = "serverconfig"
        const val SERVER_DIRECTORY_PROPERTIES = "serverdir.properties"
    }

    init {
        outputFileProperty.set(getBuildSubDir("${SERVER_DIRECTORY_PROPERTIES_DIR}/${SERVER_DIRECTORY_PROPERTIES}"))

        shareReportingDirProperty.set(createDir(getBuildSubDir("share/reportingrepository")))
        shareDistDirProperty.set(createDir(getBuildSubDir("share/dist")))
        shareImpexDirProperty.set(createDir(getBuildSubDir("share/impexschema")))

        cleanDirProperty.set(true)
    }

    /**
     * The output file contains the properties file with the complete directory configuration of ICM.
     *
     * @property outputFile real file on file system with descriptor
     */
    @get:OutputFile
    var outputFile: File
        get() = outputFileProperty.get().asFile
        set(value) = outputFileProperty.set(value)


    /**
     * Set provider for deleting existing empty dirs property.
     *
     * @param cleanDir set provider for deleting existing empty dirs property.
     */
    @Suppress( "unused")
    fun provideCleanDir(cleanDir: Provider<Boolean>) =
        cleanDirProperty.set(cleanDir)

    @get:Optional
    @get:Input
    var cleanDir by cleanDirProperty

    /**
     * Set provider for source list property. The list contains all locations with
     * sources of a project.
     *
     * @param sourceList set provider for source list property
     */
    @Suppress( "unused")
    fun provideSourceList(sourceList: ListProperty<String>) {
        sourceListPropery.set(sourceList)
    }

    fun addSource(path: String) {
        sourceListPropery.add(path)
    }

    @get:Input
    var sourceList by sourceListPropery

    /**
     * Set provider for configuration directory property.
     *
     * @param configDir set provider for configuration directory property
     */
    @Suppress( "unused")
    fun provideConfigDir(configDir: DirectoryProperty) {
        configDirProperty.set(configDir)
    }

    @get:Input
    var configDir: File
        get() = configDirProperty.get().asFile
        set(value) = configDirProperty.set(value)

    /**
     * Set provider for configuration directory property.
     *
     * @param configDir set provider for configuration directory property
     */
    @Suppress( "unused")
    fun provideSitesDir(sitesDir: DirectoryProperty) {
        sitesDirProperty.set(sitesDir)
    }

    @get:Input
    var sitesDir: File
        get() = sitesDirProperty.get().asFile
        set(value) = sitesDirProperty.set(value)

    /**
     * Set provider for share dist directory property.
     *
     * @param shareDistDir set provider for share dist directory property
     */
    @Suppress( "unused")
    fun provideShareDistDir(shareDistDir: DirectoryProperty) {
        shareDistDirProperty.set(shareDistDir)
    }

    @get:Optional
    @get:Input
    var shareDistDir: File
        get() = shareDistDirProperty.get().asFile
        set(value) = shareDistDirProperty.set(value)

    /**
     * Set provider for share reporting directory property.
     *
     * @param shareReportingDir set provider for share reporting directory property
     */
    @Suppress( "unused")
    fun provideShareReportingDir(shareReportingDir: DirectoryProperty) {
        shareReportingDirProperty.set(shareReportingDir)
    }

    @get:Optional
    @get:Input
    var shareReportingDir: File
        get() = shareReportingDirProperty.get().asFile
        set(value) = shareReportingDirProperty.set(value)

    /**
     * Set provider for share impex directory property.
     *
     * @param shareImpexgDir set provider for share impex directory property
     */
    @Suppress( "unused")
    fun provideShareImpexDir(shareImpexgDir: DirectoryProperty) {
        shareImpexDirProperty.set(shareImpexgDir)
    }

    @get:Optional
    @get:Input
    var shareImpexgDir: File
        get() = shareImpexDirProperty.get().asFile
        set(value) = shareImpexDirProperty.set(value)

    @TaskAction
    fun writeConfiguration() {
        if(outputFile.exists()) {
            outputFile.parentFile.deleteRecursively()
            outputFile.parentFile.mkdirs()
        }

        val normalizedList = ArrayList<String>()

        sourceList.forEach {
            normalizedList.add(it.replace("\\", "/"))
        }

        outputFile.printWriter().use { out ->
            out.println("#Generated properties file with server directories")
            out.println("IS_SOURCE = ${normalizedList.joinToString(separator = File.pathSeparator)}")
            out.println("IS_CLUSTER_CONFIG = ${normalizePath(getConfigSubDir("cluster"))}")
            out.println("IS_SERVLETENGINE = ${normalizePath(createDir(getConfigSubDir("system/config/servletEngine")))}")
            out.println("IS_WEBSERVICE_REPOSITORY = ${normalizePath(createDir(getConfigSubDir("webservices/repository")))}")
            out.println("IS_SITES = ${normalizePath(sitesDir)}")

            out.println("# empty server directories")
            out.println("IS_LOG = ${normalizePath(createDir(getBuildSubDir("serverlogs")))}")
            out.println("IS_TEMP = ${normalizePath(createDir(getBuildSubDir("tempdir")))}")
            out.println("IS_FONTS = ${normalizePath(createDir(getBuildSubDir("fonts")))}")

            out.println("# empty server directories - shared")
            out.println("IS_IMPEX_SCHEMA = ${normalizePath(createDir(shareImpexgDir))}")
            out.println("IS_REPORTING_REPOSITORY = ${normalizePath(createDir(shareReportingDir))}")
            out.println("IS_DIST = ${normalizePath(createDir(shareDistDir))}")
        }
    }

    private fun getBuildSubDir(path: String): File {
        return File (project.buildDir, "server/${path}")
    }

    private fun getConfigSubDir(path: String): File {
        return File (configDir, path)
    }

    private fun normalizePath(dir: File): String {
        return dir.absolutePath.replace("\\", "/")
    }

    private fun createDir(dir: File) : File {
        if(dir.exists() && cleanDir) {
            dir.deleteRecursively()
        }
        dir.mkdirs()

        return dir
    }
}
