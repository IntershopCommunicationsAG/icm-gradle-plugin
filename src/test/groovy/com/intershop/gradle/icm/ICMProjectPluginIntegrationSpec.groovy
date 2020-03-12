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
import com.intershop.gradle.test.AbstractIntegrationGroovySpec

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ICMProjectPluginIntegrationSpec extends AbstractIntegrationGroovySpec {

    def 'check configuration and dependencies'() {
        given:
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.project'
            }
            
            group = 'com.intershop.test'

            dependencies {
                ${ICMProjectPlugin.CONFIGURATION_EXTERNALCARTRIDGES} 'com.intershop.cartridge:cartridge2:1.0.0'
            }
            
            task copyConf(type: Copy) {
                from configurations.${ICMProjectPlugin.CONFIGURATION_EXTERNALCARTRIDGES}
                into "\$buildDir/result"
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("copyConf", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':copyConf').outcome == SUCCESS
        new File(testProjectDir, "build/result/cartridge2-1.0.0.jar").exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'check setup task'() {
        given:
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.project'
            }
            
            group = 'com.intershop.test'

            intershop {
                projectConfig {
                    baseProjects {
                        icm {
                            dependency = "com.intershop.icm:icm-as:1.0.0"
                            confCopySpec = project.copySpec {
                                exclude("**/**/cartridgelst.properties")
                            }
                            withCartridgeList = true
                        }
                    }
                }
            }

            dependencies {
                ${ICMProjectPlugin.CONFIGURATION_EXTERNALCARTRIDGES} 'com.intershop.cartridge:cartridge2:1.0.0'
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("setupExtCartridges", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':setupExternalCartridges').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }


}
