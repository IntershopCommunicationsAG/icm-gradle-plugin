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

    def 'check external cartridge setup task'() {
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
                            confPackage {
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
                .withArguments("setupCartridges", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':setupCartridges').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def "adapter project cartridge list properties"() {
        given:
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.project'
            }
            
            intershop {
                projectConfig {
                    cartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2' ]
                    dbprepareCartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2', "dbprep_cartr1" ]
                    productionCartridges = ['cartridge1', 'cartridge2', "dbprep_cartr1" ]

                    baseProjects {
                        icm {
                            dependency = "com.intershop.icm:icm-as:1.0.0"
                            withCartridgeList = true
                        }
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareServer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def testFile = new File(testProjectDir, "build/server/configuration/system-conf/cluster/cartridgelist.properties")
        def testProps = new Properties()

        then:
        result.task(':prepareServer').outcome == SUCCESS
        testFile.exists()
        testProps.load(testFile.newReader())
        testProps.getProperty("cartridges").contains("cartridge1 cartridge2 test_artridge1 test_cartridge2")
        testProps.getProperty("cartridges.dbinit").contains("cartridge1 cartridge2 test_artridge1 test_cartridge2 dbprep_cartr1")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("prepareContainer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def prodFile = new File(testProjectDir, "build/container/configuration/system-conf/cluster/cartridgelist.properties")
        def prodProps = new Properties()

        then:
        result1.task(':prepareContainer').outcome == SUCCESS
        prodFile.exists()
        prodProps.load(prodFile.newReader())
        prodProps.getProperty("cartridges").contains("cartridge1 cartridge2")
        prodProps.getProperty("cartridges.dbinit").contains("cartridge1 cartridge2 dbprep_cartr1")

        where:
        gradleVersion << supportedGradleVersions

    }

    def "prepare folder from release"() {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        def testFile = new File(testProjectDir, "config/base/cluster/test.properties")
        testFile.parentFile.mkdirs()
        testFile << "test = 1"

        def testFile1 = new File(testProjectDir, "config/base/cluster/cartridgelist.properties")
        testFile1.parentFile.mkdirs()
        testFile1 << "test = 2"

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.project'
            }
            
            buildDir = new File(projectDir, 'target')

            intershop {
                projectConfig {
                    cartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2' ]
                    dbprepareCartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2', "dbprep_cartr1" ]
                    productionCartridges = ['cartridge1', 'cartridge2', "dbprep_cartr1" ]

                    baseProjects {
                        icm {
                            dependency = "com.intershop.icm:icm-as:1.0.0"
                            confPackage {
                                exclude("**/**/cartridgelst.properties")
                            }
                            withCartridgeList = true
                        }
                    }

                    conf {
                        dir("config/base")
                        targetPath = "system-conf"
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareContainer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':prepareContainer').outcome == SUCCESS


        where:
        gradleVersion << supportedGradleVersions
    }

    def 'CopyLibs of subprojects to one folder'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
                id 'com.intershop.gradle.icm.project'
            }
            
            repositories {
                jcenter()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge'
        }
        
        dependencies {
            implementation 'com.google.inject:guice:4.0'
            implementation 'com.google.inject.extensions:guice-servlet:3.0'
            implementation 'javax.servlet:javax.servlet-api:3.1.0'
        
            implementation 'io.prometheus:simpleclient:0.6.0'
            implementation 'io.prometheus:simpleclient_hotspot:0.6.0'
            implementation 'io.prometheus:simpleclient_servlet:0.6.0'
            implementation 'commons-collections:commons-collections:3.2.2'
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge'
        }
        
        dependencies {
            implementation 'org.codehaus.janino:janino:2.5.16'
            implementation 'org.codehaus.janino:commons-compiler:3.0.6'
            implementation 'ch.qos.logback:logback-core:1.2.3'
            implementation 'ch.qos.logback:logback-classic:1.2.3'
            implementation 'commons-collections:commons-collections:3.2.2'
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        writeJavaTestClass("com.intershop.testCartridge1", prj1dir)
        writeJavaTestClass("com.intershop.testCartridge2", prj2dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("copyAll", '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:copyThirdpartyLibs').outcome == SUCCESS
        result.task(':testCartridge2:copyThirdpartyLibs').outcome == SUCCESS
        result.task(':copyAll').outcome == SUCCESS

        file("testCartridge2/build/lib/ch.qos.logback-logback-classic-1.2.3.jar").exists()
        file("testCartridge1/build/lib/com.google.inject-guice-4.0.jar").exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    def "prepare folder for publishing"() {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        def testFile = new File(testProjectDir, "config/base/cluster/test.properties")
        testFile.parentFile.mkdirs()
        testFile << "test = 1"

        def testFile1 = new File(testProjectDir, "config/base/cluster/cartridgelist.properties")
        testFile1.parentFile.mkdirs()
        testFile1 << "test = 2"

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.project'
            }
            
            buildDir = new File(projectDir, 'target')

            intershop {
                projectConfig {
                    cartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2' ]
                    dbprepareCartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2', "dbprep_cartr1" ]
                    productionCartridges = ['cartridge1', 'cartridge2', "dbprep_cartr1" ]

                    baseProjects {
                        icm {
                            dependency = "com.intershop.icm:icm-as:1.0.0"
                            confPackage {
                                exclude("**/**/cartridgelst.properties")
                            }
                            withCartridgeList = true
                        }
                    }

                    conf {
                        dir("config/base")
                        targetPath = "system-conf"
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareContainer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':prepareContainer').outcome == SUCCESS


        where:
        gradleVersion << supportedGradleVersions
    }
}
