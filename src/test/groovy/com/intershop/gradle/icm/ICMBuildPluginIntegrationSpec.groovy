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

import com.intershop.gradle.test.AbstractIntegrationGroovySpec
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS as SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.FAILED as FAILED

class ICMBuildPluginIntegrationSpec extends AbstractIntegrationGroovySpec {

    def 'Plugin can is applied to the root project'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm'
            }

            intershop {
            }
        """.stripIndent()

        createSubProject(":subprj1",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        createSubProject(":subprj2",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("projects")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':projects').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Plugin can not be applied to the sub project'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        createSubProject(":subprj1",
                """
                plugins {
                    id 'java'
                    id 'com.intershop.gradle.icm'
                }
                """.stripIndent())

        createSubProject(":subprj2",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("projects")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':projects').outcome == SUCCESS
        result.output.contains("ICM build plugin will be not applied to the sub project 'subprj1'")

        where:
        gradleVersion << supportedGradleVersions
    }
}
