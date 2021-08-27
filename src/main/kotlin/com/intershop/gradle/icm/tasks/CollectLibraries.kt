package com.intershop.gradle.icm.tasks

import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Collects all libraries (recursively through all (sub-)projects)
 */
open class CollectLibraries : DefaultTask() {
    private val copiedLibrariesDirectoryProperty: Property<Directory> = project.objects.directoryProperty().convention(project.layout.buildDirectory.dir(BUILD_FOLDER))

    companion object {
        const val DEFAULT_NAME = "collectLibraries"
        const val BUILD_FOLDER = "libraries"
        fun renderId(id: ModuleComponentIdentifier): String = "${id.group}-${id.module}-${id.version}"
    }

    init {
        group = "ICM server build"
        description = "Collects all libraries (recursively through all (sub-)projects)"
    }

    @get:Input
    val libraryDependencyIds: Set<String> by lazy {
        getProjectDependencies(project)
    }

    @get:OutputDirectory
    var copiedLibrariesDirectory: Directory
        get() = copiedLibrariesDirectoryProperty.get()
        set(value) = copiedLibrariesDirectoryProperty.set(value)

    /**
     * Task action starts the java process in the background.
     */
    @TaskAction
    fun collectLibraries() {

        val allDependencies = mutableMapOf<EnvironmentType, MutableSet<Library>>()
        computeProjectLibraries(project, allDependencies)

        val targetFolder = copiedLibrariesDirectory.asFile
        File(project.buildDir, BUILD_FOLDER).apply { deleteRecursively() }.apply { mkdirs() }

        allDependencies.forEach { (env, dependencies) ->
            val envFolder = File(targetFolder, env.name.lowercase()).apply { mkdirs() }
            dependencies.forEach { lib ->
                val targetName = lib.getIdString().plus(".").plus(lib.file.extension)
                val targetFile = File(envFolder, targetName)
                lib.file.copyTo(targetFile).run {
                    project.logger.debug("Copied library {} to '{}'", lib.getIdString(), this)
                }
            }
        }
    }

    /**
     * Creates a ```CopySpec``` with
     * + ```from``` = ```copiedLibrariesDirectory```
     * + ```include``` = <matching ```environmentType```>
     * + ```to``` = ```"lib"```
     */
    open fun copySpecFor(environmentType: EnvironmentType): CopySpec =
            project.copySpec { cp -> cp.from(copiedLibrariesDirectory.dir(environmentType.name.lowercase())).into("lib") }

    private fun computeProjectLibraries(project: Project, dependencies: MutableMap<EnvironmentType, MutableSet<Library>>) {
        val runtimeConfig = project.configurations.findByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        val environmentType = CartridgeUtil.getCartridgeStyle(project).environmentType()

        runtimeConfig?.run {
            resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { dependency ->
                dependency.moduleArtifacts.forEach { artifact ->
                    when (val identifier = artifact.id.componentIdentifier) {
                        is ModuleComponentIdentifier ->
                            if (!CartridgeUtil.isCartridge(project, identifier)) {
                                dependencies.computeIfAbsent(environmentType, { mutableSetOf() }).run { add(Library(identifier, artifact.file)) }
                            }
                    }
                }
            }
        }

        project.subprojects.forEach { p ->
            computeProjectLibraries(p, dependencies)
        }

        project.logger.info("Found {} transitive dependencies for project {}", dependencies.size, project.name)
    }

    /**
     * Used to support incremental builds by providing the artifact ids of all (sub-)project's (recursively). Changes lead to a new task execution.
     */
    private fun getProjectDependencies(project: Project): Set<String> {
        val runtimeConfig = project.configurations.findByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        val dependencies = mutableSetOf<String>()

        runtimeConfig?.run {
            resolvedConfiguration.lenientConfiguration.firstLevelModuleDependencies.forEach { dependency ->
                dependency.moduleArtifacts.forEach { artifact ->
                    when (val identifier = artifact.id.componentIdentifier) {
                        is ModuleComponentIdentifier ->
                            if (!CartridgeUtil.isCartridge(project, identifier)) {
                                dependencies.add(renderId(identifier))
                            }
                    }
                }
            }
        }

        project.subprojects.forEach { p ->
            dependencies.addAll(getProjectDependencies(p))
        }

        project.logger.info("Found {} first level module dependencies for project {}", dependencies.size, project.name)
        return dependencies
    }

    open class Library constructor(val id: ModuleComponentIdentifier, val file: File) {

        fun getIdString(): String = renderId(id)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Library

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString(): String {
            return "Library(id=$id, file=$file)"
        }

    }


}

