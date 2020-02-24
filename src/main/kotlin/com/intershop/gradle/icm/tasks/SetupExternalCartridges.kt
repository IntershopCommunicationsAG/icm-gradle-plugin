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

import com.intershop.gradle.icm.ICMProjectPlugin.Companion.CONFIGURATION_EXTERNALCARTRIDGES
import com.intershop.gradle.icm.extension.IntershopExtension
import com.intershop.gradle.icm.extension.ProjectConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.result.DefaultResolvedArtifactResult
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File
import javax.inject.Inject

abstract class SetupExternalCartridges: DefaultTask() {

    companion object {
        const val DEFAULT_NAME = "setupExternalCartridges"
    }

    @get:Inject
    abstract val fsOps: FileSystemOperations

    @get:Inject
    abstract val objectFactory: ObjectFactory

    @get:Inject
    abstract val projectLayout: ProjectLayout

    private val cartridgeDirProperty: DirectoryProperty = objectFactory.directoryProperty()

    init {
        group = IntershopExtension.INTERSHOP_GROUP_NAME
        description = "Create a directory with external cartridges."

        cartridgeDirProperty.convention(projectLayout.buildDirectory.dir(ProjectConfiguration.EXTERNAL_CARTRIDGE_PATH))
    }

    @get:Input
    val externalCartridgeDependencies: List<String> by lazy {
        val returnDeps = mutableListOf<String>()
        project.configurations.getByName(CONFIGURATION_EXTERNALCARTRIDGES).dependencies.forEach {
            returnDeps.add(it.toString())
        }
        returnDeps
    }

    fun provideCartridgeDir(cartridgeDir: Provider<Directory>) = cartridgeDirProperty.set(cartridgeDir)

    @get:OutputDirectory
    var cartridgeDir: File
        get() = cartridgeDirProperty.get().asFile
        set(value) = cartridgeDirProperty.set(value)

    private fun createStructure(target: File) {
        val cfg = project.configurations.getByName(CONFIGURATION_EXTERNALCARTRIDGES)

        cfg.allDependencies.forEach { dependency ->
            if( dependency is ExternalModuleDependency) {
                project.logger.info("Process external cartridge '{}'.", dependency.name)
                var staticFile = getStaticFileFor(dependency)
                fsOps.run {
                    copy { it: CopySpec ->
                        it.from(project.zipTree(staticFile))
                        it.into(File(target, "${dependency.name}/release"))
                    }
                }
                project.logger.info("{}: Process static file {}.", dependency.name, staticFile)
                var jarFile = getJarFileFor(dependency)
                fsOps.run {
                    copy { it: CopySpec ->
                        it.from(jarFile)
                        it.into(File(target, "${dependency.name}/release/lib/"))
                    }
                }
                project.logger.info("{}: Process jar file {}.", dependency.name, jarFile)
                var pomFile = getPomFileFor(dependency)

                project.logger.info("{}: Process jar dependencies {}.", dependency.name, pomFile)
                var libFiles = getLibsFor(dependency)
                libFiles.forEach { lib ->
                    project.logger.info("{}: Copy {} to {}.", dependency.name, lib.key, lib.value)
                    fsOps.run {
                        copy { it: CopySpec ->
                            it.from(lib.key)
                            it.into(File(target, "libs"))
                            it.rename(lib.key.name, lib.value)
                        }
                    }
                }

            }
        }
    }

    private fun getStaticFileFor(dependency: ExternalModuleDependency): File {
        var dep = dependency.copy()
        dep.artifact {
            it.name = dep.name
            it.classifier = "staticfiles"
            it.extension = "zip"
            it.type = "zip"
        }
        var dcfg = project.configurations.detachedConfiguration(dep)
        dcfg.setTransitive(false)
        var files = dcfg.resolve()

        return files.first()
    }

    private fun getJarFileFor(dependency: ExternalModuleDependency): File {
        var dep = dependency.copy()
        var dcfg = project.configurations.detachedConfiguration(dep)
        dcfg.setTransitive(false)
        var files = dcfg.resolve()

        return files.first()
    }

    private fun getPomFileFor(dependency: ExternalModuleDependency): File {
        val compID = DefaultModuleComponentIdentifier.newId(
            DefaultModuleIdentifier.newId(dependency.group!!, dependency.name),
            dependency.version)
        val mavenArtifacts = project.dependencies.createArtifactResolutionQuery()
            .forComponents(compID)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
            .execute()

        var rv: File? = null
        mavenArtifacts.resolvedComponents.forEach {
            if(it.id is ModuleComponentIdentifier) {
                it.getArtifacts(MavenPomArtifact::class.java).forEach {
                    rv = (it as DefaultResolvedArtifactResult).file
                }
            }
        }
        return rv!!
    }

    private fun getLibsFor(dependency: ExternalModuleDependency): Map<File, String> {
        val files  = mutableMapOf<File, String>()

        var dep = dependency.copy()
        var dcfg = project.configurations.detachedConfiguration(dep)
        dcfg.setTransitive(true)

        dcfg.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.id is DefaultModuleComponentArtifactIdentifier) {
                val identifier = artifact.id
                if(identifier is DefaultModuleComponentArtifactIdentifier) {
                    val name = "${identifier.componentIdentifier.group}-" +
                            "${identifier.componentIdentifier.module}-" +
                            "${identifier.componentIdentifier.version}.${artifact.type}"

                    if(! CartridgeUtil.isCartridge(project, identifier.componentIdentifier)) {
                        files.put(artifact.file, name)
                    }
                } else {
                    throw GradleException("Artifact ID is not a module identifier.")
                }
            }
        }

        return files
    }

    @TaskAction
    fun processDependencies() {
        createStructure(cartridgeDirProperty.get().asFile)
    }

}