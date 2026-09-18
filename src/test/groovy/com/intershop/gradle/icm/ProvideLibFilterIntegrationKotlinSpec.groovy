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

import com.intershop.gradle.icm.util.TestRepo
import com.intershop.gradle.test.AbstractIntegrationKotlinSpec

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Tests for the ProvideLibFilter task, registered as 'provideLibFilter' by the ICMProjectPlugin.
 *
 * The test repository provides 'com.intershop.icm:icm-as:1.0.0' with a 'libs'/'txt' artifact and
 * 'com.intershop.icm:icm-as:2.0.0' without one, so both the resolvable and the unresolvable case
 * can be verified.
 */
class ProvideLibFilterIntegrationKotlinSpec extends AbstractIntegrationKotlinSpec {

    private String buildFileFor(String baseDependency, String libFilterFileDependency) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        String libFilterLine = libFilterFileDependency == null ? "" :
                """libFilterFileDependency.set("${libFilterFileDependency}")"""

        return """
            plugins {
                id("com.intershop.gradle.icm.project")
            }

            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {
                    base {
                        dependency.set("${baseDependency}")
                    }
                    ${libFilterLine}
                }
            }

            ${repoConf}
        """.stripIndent()
    }

    def 'writes the downloaded lib filter file for the base dependency'() {
        given:
        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        buildFile << buildFileFor("com.intershop.icm:icm-as:1.0.0", null)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("provideLibFilter", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        File libFilterFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        result.task(':provideLibFilter').outcome == SUCCESS
        libFilterFile.exists()
        libFilterFile.text.contains("com.fasterxml.jackson.core-jackson-core-2.9.10")
        libFilterFile.text.contains("wsdl4j-wsdl4j-1.6.3")

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'writes an empty lib filter file if the dependency provides no lib filter artifact'() {
        given:
        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        buildFile << buildFileFor("com.intershop.icm:icm-as:2.0.0", null)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("provideLibFilter", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        File libFilterFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then: 'the build does not fail, an empty file is created instead'
        result.task(':provideLibFilter').outcome == SUCCESS
        libFilterFile.exists()
        libFilterFile.text.isEmpty()

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'prefers the explicitly configured lib filter file dependency over the base dependency'() {
        given:
        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        // the base dependency provides no lib filter, the file dependency does
        buildFile << buildFileFor("com.intershop.icm:icm-as:2.0.0", "com.intershop.icm:icm-as:1.0.0")

        when:
        def result = getPreparedGradleRunner()
                .withArguments("provideLibFilter", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        File libFilterFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        result.task(':provideLibFilter').outcome == SUCCESS
        libFilterFile.exists()
        libFilterFile.text.contains("com.fasterxml.jackson.core-jackson-core-2.9.10")

        where:
        gradleVersion << supportedGradleVersions
    }
}
