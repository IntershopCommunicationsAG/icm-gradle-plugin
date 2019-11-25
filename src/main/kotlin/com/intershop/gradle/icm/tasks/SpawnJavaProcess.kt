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
import org.apache.tools.ant.types.Commandline
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.JavaDebugOptions
import org.gradle.process.internal.DefaultJavaDebugOptions
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Generic task for starting a java process in the background.
 *
 * @constructor initializes object factory.
 */
open class SpawnJavaProcess: DefaultTask() {

    companion object {
        private const val XMS_PREFIX = "-Xms"
        private const val XMX_PREFIX = "-Xmx"
    }

    private val mainProperty: Property<String> = project.objects.property(String::class.java)
    private val readyStringProperty: Property<String> = project.objects.property(String::class.java)
    private val minHeapSizeProperty: Property<String> = project.objects.property(String::class.java)
    private val maxHeapSizeProperty: Property<String> = project.objects.property(String::class.java)

    private val pidFileProperty: RegularFileProperty = project.objects.fileProperty()
    private val workingDirProperty: RegularFileProperty = project.objects.fileProperty()

    private val classpathProperty: ConfigurableFileCollection = project.files()

    private val jvmArgsListProperty: ListProperty<String> = project.objects.listProperty(String::class.java)
    private val argsListProperty: ListProperty<String> = project.objects.listProperty(String::class.java)

    private val environmentMapProperty: MapProperty<String, Any> =
        project.objects.mapProperty(String::class.java, Any::class.java)
    private val systemPropertiesMapProperty: MapProperty<String, Any> =
        project.objects.mapProperty(String::class.java, Any::class.java)

    private val debugProperty: Property<Boolean> = project.objects.property(Boolean::class.java)

    private val debugOptionsInternal = DefaultJavaDebugOptions()

    private var processIsReady = false

    init {
        workingDirProperty.convention(project.layout.buildDirectory.file("activeProcess"))
        minHeapSizeProperty.set("")
        maxHeapSizeProperty.set("")
        debugProperty.set(false)
        pidFileProperty.convention { File(project.buildDir, "start/javaprocess") }
    }

    /**
     * Set provider for main class property.
     *
     * @param mainClass set provider for main class.
     */
    @Suppress("unused")
    fun provideMain(mainClass: Provider<String>) =
        mainProperty.set(mainClass)

    @get:Input
    var main by mainProperty

    /**
     * Set provider for a ready string property.
     *
     * @param readyString set provider for ready string.
     */
    @Suppress("unused")
    fun provideReadyString(readyString: Provider<String>) =
        readyStringProperty.set(readyString)

    @get:Input
    var readyString by readyStringProperty

    /**
     * Set provider for minHeapSize string property.
     *
     * @param minHeapSize minimum memory size.
     */
    @Suppress("unused")
    fun providerMinHeapSize(minHeapSize: Provider<String>) =
        minHeapSizeProperty.set(minHeapSize)

    @get:Optional
    @get:Input
    var minHeapSize: String
        get() = minHeapSizeProperty.getOrElse("")
        set(value) = minHeapSizeProperty.set(value)

    /**
     * Set provider for maxHeapSize string property.
     *
     * @param maxHeapSize maximum memory size.
     */
    @Suppress("unused")
    fun providerMaxHeapSize(maxHeapSize: Provider<String>) =
        maxHeapSizeProperty.set(maxHeapSize)

    @get:Optional
    @get:Input
    var maxHeapSize: String
        get() = maxHeapSizeProperty.getOrElse("")
        set(value) = maxHeapSizeProperty.set(value)

    /**
     * The workingDir is used for the execution of the java process.
     *
     * @property processDirProperty real directory on file system
     */
    @get:InputDirectory
    var workingDir: File
        get() {
            var dir = workingDirProperty.get().asFile
            if(! dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }
        set(value) = workingDirProperty.set(value)

    /**
     * The file will contain the pid of the process. As long this file
     * exists, it should be not possible to spawn a new prcess.
     *
     * @property pidFile real file on file system with pid
     */
    @get:OutputFile
    var pidFile: File
        get() = pidFileProperty.get().asFile
        set(value) = pidFileProperty.set(value)

    /**
     * This file collection contains the main class and all necessary
     * class files for execution.
     *
     * @property classpath file collection with classpath
     */
    @get:Classpath
    var classpath: FileCollection
        get() = classpathProperty
        set(value) {
            classpathProperty.from(value)
        }

    /**
     * Set provider for JVM arguments.
     *
     * @param jvmArgs list of JVM arguments.
     */
    @Suppress("unused")
    fun providerJvmArgs(jvmArgs: Provider<List<String>>) =
        jvmArgsListProperty.set(jvmArgs)

    /**
     * This list contains parameters for the jvm.
     *
     * @property jvmArgs list of jvm parameters
     */
    @get:Input
    var jvmArgs by jvmArgsListProperty

    /**
     * Add JVM args to the list of JVM args.
     *
     * @param jvmArgs one ore more JVM arguments.
     */
    fun jvmArgs(vararg jvmArgs: Any) {
        jvmArgs.forEach {
            jvmArgsListProperty.add(it.toString())
        }
    }

    /**
     * Add a list of JVM args to the list of JVM args.
     *
     * @param jvmArgs list of JVM arguments.
     */
    fun jvmArgs(jvmArgs: Iterable<Any>) {
        jvmArgs.forEach {
            jvmArgsListProperty.add(it.toString())
        }
    }

    /**
     * Set provider for arguments passed to the main class.
     *
     * @param args list of arguments.
     */
    @Suppress("unused")
    fun providerArgs(args: Provider<List<String>>) =
        argsListProperty.set(args)

    /**
     * This list contains parameters passed to the main java class.
     *
     * @property args list of program parameters
     */
    @get:Input
    var args by argsListProperty

    /**
     * Add args to the list of args passed to the main class.
     *
     * @param args one ore more arguments.
     */
    fun args(vararg args: Any) {
        args.forEach {
            argsListProperty.add(it.toString())
        }
    }

    /**
     * Add a list of args to the list of args passed to the main class.
     *
     * @param args list of arguments.
     */
    fun args(args: Iterable<Any>) {
        args.forEach {
            argsListProperty.add(it.toString())
        }
    }

    /**
     * Command line arguments passed to the main class.
     * This list will replace the configured parameters.
     *
     * @param args string of parameters.
     */
    @Option(option = "args", description = "Command line arguments passed to the main class.")
    fun argsString(args: String) {
        argsListProperty.set(Commandline.translateCommandline(args).toList())
    }

    /**
     * Set provider for environment.
     *
     * @param environment provider for environment map.
     */
    @Suppress("unused")
    fun providerEnvironment(environment: Provider<Map<String, Any>>) =
        environmentMapProperty.set(environment)

    /**
     * This map contains environment properties.
     *
     * @property environment map of environment properties
     */
    @get:Input
    var environment: Map<String, Any>
        get() = environmentMapProperty.get()
        set(value) {
            environmentMapProperty.set(value)
        }

    /**
     * Add a map of environment variables to the map of environment variables.
     *
     * @param env map of environment variables.
     */
    fun environment(env: Map<String, Any>) {
        env.forEach {
            environmentMapProperty.put(it.key, it.value)
        }
    }

    /**
     * Add a new key value pair to the environment variables.
     *
     * @param key name of environment variable.
     * @param value value of environment variable.
     */
    fun environment(key: String, value: Any) {
        environmentMapProperty.put(key, value)
    }

    /**
     * Set provider for system properties.
     *
     * @param systemProperties provider for system properties map.
     */
    @Suppress("unused")
    fun providerSystemProperties(systemProperties: Provider<Map<String, Any>>) =
        systemPropertiesMapProperty.set(systemProperties)

    /**
     * This map contains system properties.
     *
     * @property systemProperties map of system properties
     */
    @get:Input
    var systemProperties: Map<String, Any>
        get() = systemPropertiesMapProperty.get()
        set(value) {
            systemPropertiesMapProperty.set(value)
        }

    /**
     * Add a map of system properties to the map of system properties.
     *
     * @param env map of environment variables.
     */
    fun systemProperties(props: Map<String, Any>) {
        props.forEach {
            systemPropertiesMapProperty.put(it.key, it.value)
        }
    }

    /**
     * Add a new key value pair to the map of system properties.
     *
     * @param key name of system property.
     * @param value value of system property.
     */
    fun systemProperty(key: String, value: Any) {
        systemPropertiesMapProperty.put(key, value)
    }

    /**
     * Enable debugging for the process. The process is started suspended and listening on port 5005.
     * This can be configured also over the gradle parameter "debug-java".
     *
     * @property debug is the task property
     */
    @get:Option(
        option = "debug-jvm",
        description = "Enable debugging for the process. The process is started suspended and listening on port 5005."
    )
    @get:Input
    val debug by debugProperty

    /**
     * This get the available debug configuration for the java process.
     */
    @get:Internal
    val debugOptions : JavaDebugOptions
        get() = debugOptionsInternal

    /**
     * Configures the debug options for this java process.
     *
     * @param action new configuration for debug options.
     */
    fun debugOptions(action: Action<JavaDebugOptions>) {
        action.execute(debugOptionsInternal)
    }

    private fun buildProcess(): Process {
        val javaExecFile = org.gradle.internal.jvm.Jvm.current().getJavaExecutable()


        val command = mutableListOf<String>()

        command.add(javaExecFile.absolutePath)
        command.add("-cp")
        command.add(classpath.asPath)

        if (minHeapSizeProperty.get() != "") {
            command.add(XMS_PREFIX + minHeapSizeProperty.get());
        }
        if (maxHeapSizeProperty.get() != "") {
            command.add(XMX_PREFIX + maxHeapSizeProperty.get());
        }

        addAllSystemProperties(command)

        command.add(mainProperty.get())
        command.addAll(args)

        if(debugProperty.get() == true) {
            val server = debugOptionsInternal.getServer().get();
            val suspend = debugOptionsInternal.getSuspend().get();
            val port = debugOptionsInternal.getPort().get();

            var param = "-agentlib:jdwp=transport=dt_socket,"
            param = if(server) {
                param + "server=y"
            } else {
                param + "server=n"
            }
            param = if(suspend) {
                param + ",suspend=y"
            } else {
                param + ",suspend=n"
            }

            param = param + ",address=" + port

            command.add(param)
        }

        project.logger.info("Command: " + command.joinToString())

        val builder = ProcessBuilder(command)
        builder.redirectErrorStream(true)
        configureEnvironment(builder.environment())

        builder.directory(configureWorkingDir())

        return builder.start()
    }

    private fun addAllSystemProperties(command: MutableList<String>) {
        systemPropertiesMapProperty.get().forEach {
            command.add("-D${it.key}=${getValueString(it.value)}")
        }
    }

    private fun configureEnvironment(map: MutableMap<String,String>)  {
        val javaHome = org.gradle.internal.jvm.Jvm.current().javaHome

        environmentMapProperty.get().forEach {
            map.put( it.key, getValueString(it.value) )
        }

        map.put("JAVA_HOME", javaHome.absolutePath)
    }

    fun getValueString(v: Any) : String = when(v) {
        is File -> v.absolutePath
        is Boolean -> if(v) "true" else "false"
        else -> v.toString()
    }

    private fun configureWorkingDir() : File {
        val workingDir = workingDirProperty.get().asFile

        if(! workingDir.exists()) {
            workingDir.mkdirs()
        }

        return workingDir
    }

    private fun extractPidFromProcess(process: Process): Int {
        val pidField = process.javaClass.getDeclaredField("pid")
        pidField.isAccessible = true
        return pidField.getInt(process)
    }

    /**
     * Task action starts the java process in the background.
     */
    @TaskAction
    fun spawnProcess() {

        val process = buildProcess()

        val stdout = process.getInputStream()
        val reader = BufferedReader(InputStreamReader(stdout))

        var line: String?
        do {
            line = reader.readLine()
            if (line != null) {
                println(line)
                if (line.contains(readyStringProperty.get())) {
                    println("command is ready")
                    processIsReady = true
                    break
                }
            } else {
                break
            }
        } while (true)

        if (processIsReady) {
            pidFileProperty.get().asFile.printWriter().use { out -> out.println(extractPidFromProcess(process)) }
        }
    }
}
