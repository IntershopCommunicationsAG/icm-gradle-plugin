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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CreateClusterIDSpec extends AbstractIntegrationGroovySpec {

    def "clusterID will be executed"() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.base'
            }

            """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("tasks", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':tasks').outcome == SUCCESS


        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("createClusterID", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':createClusterID').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }
}
