package com.intershop.gradle.icm.tasks

import com.intershop.gradle.icm.utils.CartridgeUtil
import com.intershop.gradle.icm.utils.EnvironmentType
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

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
    val allDependencyIds: String by lazy {
        libraryDependencyIds.toString()
    }

    @get:Internal
    val libraryDependencyIds: Map<EnvironmentType, List<String>> by lazy {
        val dependencies = mutableMapOf<EnvironmentType, MutableSet<String>>()

        dependsOn.forEach { t ->
            val p = t as TaskProvider<*>
            when (val at = p.get()) {
                is WriteCartridgeDescriptor -> {
                    val environmentType = CartridgeUtil.getCartridgeStyle(at.project).environmentType()
                    dependencies.computeIfAbsent(environmentType, { linkedSetOf() }).addAll(at.getLibraryIDs())
                }
            }
        }

        // ensure ids for a certain EnvironmentType only contain ids of this type not others (e.g. test without production)
        val alreadyThere = mutableSetOf<String>()
        EnvironmentType.values().forEach { env ->
            dependencies.get(env)?.run {
                this.removeAll(alreadyThere)
                alreadyThere.addAll(this)
            }
        }

        val dependencyWithList = mutableMapOf<EnvironmentType, List<String>>()
        dependencies.forEach{ (k, v) -> dependencyWithList.put(k, v.toList().sorted()) }

        dependencyWithList.toSortedMap()
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
        copiedLibrariesDirectory.asFile.deleteRecursively()
        libraryDependencyIds.map { (key, value) ->
            copySpecFor(key, value)
        }.forEach { copySpec ->
            project.copy { cs -> cs.with(copySpec).into(copiedLibrariesDirectory) }
        }
    }

    /**
     * Creates a ```CopySpec``` which describes that libraries get copied into the folder '''libraries/{environmentName}''' using file-name '''${dependency.moduleGroup}_${dependency.moduleName}_${dependency.moduleVersion}.${artifact.extension}'''
     */
    private fun copySpecFor(environmentType: EnvironmentType, ids: Collection<String>): CopySpec {

        val libCopySpec = project.copySpec().into(environmentType.name.lowercase())
        val configuration = project.configurations.create("CollectedLibraries${environmentType.name}")
        ids.forEach({ configuration.dependencies.add(project.dependencies.create(it)) })
        configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { dependency ->
            dependency.moduleArtifacts.forEach { artifact ->
                when (artifact.id.componentIdentifier) {
                    is ModuleComponentIdentifier -> {
                        libCopySpec.with(project.copySpec().from(artifact.file).rename({ name -> "${dependency.moduleGroup}_${dependency.moduleName}_${dependency.moduleVersion}.${artifact.extension}" }))
                    }
                }
            }
        }
        return libCopySpec
    }

    /**
     * Creates a ```CopySpec``` with
     * + ```from``` = ```copiedLibrariesDirectory```
     * + ```include``` = <matching ```environmentType```>
     * + ```to``` = ```"lib"```
     */
    open fun copySpecFor(environmentType: EnvironmentType): CopySpec =
            project.copySpec { cp -> cp.from(copiedLibrariesDirectory.dir(environmentType.name.lowercase())).into("lib") }

}

