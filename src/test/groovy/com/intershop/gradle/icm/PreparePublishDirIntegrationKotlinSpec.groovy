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
package com.intershop.gradle.icm

import com.intershop.gradle.test.AbstractIntegrationKotlinSpec

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Tests for the PreparePublishDir task. The task is not registered by one of the plugins, therefore it is
 * configured directly in the test build scripts.
 */
class PreparePublishDirIntegrationKotlinSpec extends AbstractIntegrationKotlinSpec {

    private static final String ROOT_PROJECT_SETTINGS = """
        rootProject.name="rootproject"
        """.stripIndent()

    private static final String OUTPUT_DIR = "build/publish"

    private void createSourceFiles() {
        File baseDir = new File(testProjectDir, "src/base")
        baseDir.mkdirs()
        new File(baseDir, "base.properties") << "base=1"
        new File(baseDir, "base.log") << "should be excluded"

        File baseSubDir = new File(baseDir, "sub")
        baseSubDir.mkdirs()
        new File(baseSubDir, "nested.properties") << "nested=1"

        File extraDir = new File(testProjectDir, "src/extra")
        extraDir.mkdirs()
        new File(extraDir, "extra.properties") << "extra=1"
    }

    /**
     * Builds a test build script registering a {@code PreparePublishDir} task. Only the configuration of the
     * base server directory differs between the tests, so it is handed in.
     *
     * @param baseServerDirConfig statements applied to the base server directory configuration itself
     * @param baseDirConfig statements applied to the directory configuration within the base server directory
     */
    private static String buildFileFor(String baseServerDirConfig, String baseDirConfig) {
        """
            import com.intershop.gradle.icm.extension.ServerDir
            import com.intershop.gradle.icm.tasks.PreparePublishDir

            plugins {
                id("com.intershop.gradle.icm.base")
            }

            val baseConfig = objects.newInstance(ServerDir::class.java, "", listOf<String>(), listOf<String>())
            ${baseServerDirConfig}
            baseConfig.dirs.create("base") {
                dir.set(file("src/base"))
                ${baseDirConfig}
            }

            val extraConfig = objects.newInstance(ServerDir::class.java, "", listOf<String>(), listOf<String>())
            extraConfig.dirs.create("extra") {
                dir.set(file("src/extra"))
            }

            tasks.register<PreparePublishDir>("preparePublish") {
                baseDirConfig.set(baseConfig)
                extraDirConfig.set(extraConfig)
                outputDirectory.set(layout.buildDirectory.dir("publish"))
            }
        """.stripIndent()
    }

    /**
     * Runs the {@code preparePublish} task of a prepared test project.
     *
     * @param gradleVersion the Gradle version to run the build with
     */
    private def runPreparePublish(String gradleVersion) {
        getPreparedGradleRunner()
                .withArguments("preparePublish", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()
    }

    def 'copies the configured directories to the output directory'() {
        given:
        createSourceFiles()

        settingsFile << ROOT_PROJECT_SETTINGS

        buildFile << buildFileFor('baseConfig.exclude("**/*.log")', "")

        when:
        def result = runPreparePublish(gradleVersion)

        File outputDir = new File(testProjectDir, OUTPUT_DIR)

        then:
        result.task(':preparePublish').outcome == SUCCESS
        new File(outputDir, "base.properties").exists()
        new File(outputDir, "sub/nested.properties").exists()
        new File(outputDir, "extra.properties").exists()
        ! new File(outputDir, "base.log").exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'applies the target paths of the directory and the server directory configuration'() {
        given:
        createSourceFiles()

        settingsFile << ROOT_PROJECT_SETTINGS

        buildFile << buildFileFor('baseConfig.target.set("serverTarget")',
                '''target.set("dirTarget")
                exclude("**/*.log")''')

        when:
        def result = runPreparePublish(gradleVersion)

        File outputDir = new File(testProjectDir, OUTPUT_DIR)

        then:
        result.task(':preparePublish').outcome == SUCCESS
        // the target of the directory configuration is resolved relative to the target of the server dir
        new File(outputDir, "serverTarget/dirTarget/base.properties").exists()
        new File(outputDir, "serverTarget/dirTarget/sub/nested.properties").exists()
        ! new File(outputDir, "serverTarget/dirTarget/base.log").exists()
        // a server dir without a target is copied to the root of the output directory
        new File(outputDir, "extra.properties").exists()

        where:
        gradleVersion << supportedGradleVersions
    }
}
