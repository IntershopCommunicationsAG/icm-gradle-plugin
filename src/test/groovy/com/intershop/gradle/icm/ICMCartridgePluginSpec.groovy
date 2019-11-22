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

class ICMCartridgePluginSpec extends AbstractIntegrationGroovySpec {

    def 'ZipFile will be created and published'() {

        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'maven-publish'
                id 'com.intershop.gradle.icm.base'
            }

            def publishDir = "\$buildDir/repo"  

            group = "com.intershop"
            version = "1.0.0"

            subprojects {

                group = "com.intershop"
                version = "1.0.0"

                publishing {
                    repositories {
                        maven {
                            // change to point to your repo, e.g. http://my.org/repo
                            url = publishDir
                        }
                    }
                }
            }
            
            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = publishDir
                    }
                }
            }
        """.stripIndent()

        File prjDir1 = createSubProject(":subprj1",
                """
                plugins {
                    id 'com.intershop.gradle.icm.cartridge'
                    id 'java'
                }
                """.stripIndent())

        writeJavaTestClass("com.intershop.test.Test1", prjDir1)
        file("staticfiles/cartridge/logback/logback-test.xml", prjDir1)

        File prjDir2 = createSubProject(":subprj2",
                """
                plugins {
                    id 'com.intershop.gradle.icm.cartridge'
                    id 'java'
                }
                """.stripIndent())

        writeJavaTestClass("com.intershop.test.Test2", prjDir2)
        file("staticfiles/cartridge/test/test.properties", prjDir2)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("publish", "-s")
                //.withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':publish').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }
}
