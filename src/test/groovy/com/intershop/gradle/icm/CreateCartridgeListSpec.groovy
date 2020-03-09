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
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class CreateCartridgeListSpec extends AbstractIntegrationGroovySpec {

    def "createCartridgeList will be executed with excludes"() {
        given:
        copyResources("cartridgelist", "cartridgelist")

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            task createCartridgeList(type: com.intershop.gradle.icm.tasks.CreateCartridgeList) { 
                exclude(".*_test\\\$")
                exclude("^dev_.*") 
                exclude("^etest.*")
                exclude("^tool_webtest.*")
                exclude("^pmc_unit_testing.*")
                
                templateFile = file("cartridgelist/cartridgelist.properties")
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
                .withArguments("createCartridgeList", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':createCartridgeList').outcome == SUCCESS

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("createCartridgeList", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':createCartridgeList').outcome == UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    def "createCartridgeList will be executed with includes and excludes"() {
        given:
        copyResources("cartridgelist", "cartridgelist")

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            task createCartridgeList(type: com.intershop.gradle.icm.tasks.CreateCartridgeList) { 
                excludes = [ "^dev_.*" ]
                
                templateFile = file("cartridgelist/cartridgelist.properties")
            }

            """.stripIndent()

        def file = new File(testProjectDir, "build/cartridgelist/cartridgelist.properties")

                when:
        def result = getPreparedGradleRunner()
                .withArguments("tasks", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':tasks').outcome == SUCCESS


        when:
        buildFile.delete()
        buildFile.createNewFile()

        buildFile << """
            plugins {
             id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            task createCartridgeList(type: com.intershop.gradle.icm.tasks.CreateCartridgeList) { 
                excludes = [ "^dev_.*" ]
            
                include("dev_query")
                
                templateFile = file("cartridgelist/cartridgelist.properties")
            }

            """.stripIndent()

        def result2 = getPreparedGradleRunner()
                .withArguments("createCartridgeList", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':createCartridgeList').outcome == SUCCESS
        file.exists()

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("createCartridgeList", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':createCartridgeList').outcome == UP_TO_DATE
        file.exists()
        file.getText().contains("dev_query")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "createCartridgeList will be executed with complex configuration"() {
        given:
        copyResources("cartridgelist", "cartridgelist")

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            task createCartridgeList(type: com.intershop.gradle.icm.tasks.CreateCartridgeList) { 
                excludes = [ ".*_test\\\$", "^dev_.*", "^etest.*", "^tool_webtest.*", "^pmc_unit_testing.*", "^test_.*"]
                
                templateFile = file("cartridgelist/cartridgelist.properties")
            }

            """.stripIndent()

        def file = new File(testProjectDir, "build/cartridgelist/cartridgelist.properties")

        when:
        def result = getPreparedGradleRunner()
                .withArguments("createCartridgeList", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':createCartridgeList').outcome == SUCCESS
        file.text.contains("init_contactcenter")
        ! file.text.contains("init_contactcenter \\")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "extended cartridge list properties"() {
        given:
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        copyResources("cartridgelist", "build/input")

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

                    configurationPackage = "com.intershop.icm:icm-as:1.0.0"

                    sitesPackage = "com.intershop.icm:icm-as:1.0.0"
                }
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("extendCartrideListTest", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def testFile = new File(testProjectDir, "build/test/cartridgelist.properties")
        def testProps = new Properties()

        then:
        result.task(':extendCartrideListTest').outcome == SUCCESS
        testFile.exists()
        testProps.load(testFile.newReader())
        testProps.getProperty("cartridges").contains("cartridge1 cartridge2 test_artridge1 test_cartridge2")
        testProps.getProperty("cartridges.dbinit").contains("cartridge1 cartridge2 test_artridge1 test_cartridge2 dbprep_cartr1")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("extendCartrideListProd", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def prodFile = new File(testProjectDir, "build/production/cartridgelist.properties")
        def prodProps = new Properties()

        then:
        result1.task(':extendCartrideListProd').outcome == SUCCESS
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

        def sitesFile = new File(testProjectDir, "sites/base/test/site.properties")
        sitesFile.parentFile.mkdirs()
        sitesFile << "test = 1"

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.project'
            }
            
            intershop {
                projectConfig {
                    cartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2' ]
                    dbprepareCartridges = ['cartridge1', 'cartridge2', 'test_artridge1', 'test_cartridge2', "dbprep_cartr1" ]
                    productionCartridges = ['cartridge1', 'cartridge2', "dbprep_cartr1" ]

                    configurationPackage = "com.intershop.icm:icm-as:1.0.0"
                    sitesPackage = "com.intershop.icm:icm-as:1.0.0"

                    sitesDir = file("sites/")
                    configDir = file("config/")
                }
            }

            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareFolders", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':prepareFolders').outcome == SUCCESS

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("syncFolders", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':syncFolders').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }
}
