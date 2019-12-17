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

class ICMCatridgeTypeSpec extends AbstractIntegrationGroovySpec {

    def 'Check project for isTestCartridge property'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.testCartridge'
            }
            
            tasks.register("testtask") { 
                doLast { 
                    if(project.hasProperty('isTestCartridge')) {
                        println "isTestCartridge exists \${project.property('isTestCartridge')}"
                    } else {
                        println "isTestCartridge does not exist"
                    }
                }
            }""".stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("testtask", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testtask').outcome == SUCCESS
        result.output.contains("isTestCartridge exists true")

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Check project for isDevCartridge property'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.devCartridge'
            }
            
            tasks.register("testtask") { 
                doLast { 
                    if(project.hasProperty('isDevCartridge')) {
                        println "isDevCartridge exists \${project.property('isDevCartridge')}"
                    } else {
                        println "isDevCartridge does not exist"
                    }
                }
            }""".stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("testtask", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testtask').outcome == SUCCESS
        result.output.contains("isDevCartridge exists true")

        where:
        gradleVersion << supportedGradleVersions
    }
}
