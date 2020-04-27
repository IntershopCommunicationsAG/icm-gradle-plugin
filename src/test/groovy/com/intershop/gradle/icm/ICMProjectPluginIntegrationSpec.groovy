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

    def 'check lic configuration from extension'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            task showLicPath() { 
                doLast {
                    println(project.extensions.getByName("intershop").developmentConfig.licenseFilePath)
                }
            }

            """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':showLicPath').outcome == SUCCESS
        result.output.contains(".gradle/icm-default/lic/license.xml")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s", "-DlicenseDir=/home/user/licdir")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showLicPath').outcome == SUCCESS
        result1.output.contains("/home/user/licdir/license.xml")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s", "-PlicenseDir=/home/otheruser/licdir")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showLicPath').outcome == SUCCESS
        result2.output.contains("/home/otheruser/licdir/license.xml")

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s")
                .withEnvironment([ "LICENSEDIR": "/home/other/licdir" ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showLicPath').outcome == SUCCESS
        result3.output.contains("/home/other/licdir/license.xml")

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'check conf configuration from extension'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            task showConfPath() { 
                doLast {
                    println(project.extensions.getByName("intershop").developmentConfig.configFilePath)
                }
            }

            """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':showConfPath').outcome == SUCCESS
        result.output.contains(".gradle/icm-default/conf/cluster.properties")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "-DconfigDir=/home/user/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showConfPath').outcome == SUCCESS
        result1.output.contains("/home/user/conf/cluster.properties")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "-PconfigDir=/home/otheruser/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showConfPath').outcome == SUCCESS
        result2.output.contains("/home/otheruser/conf/cluster.properties")

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s")
                .withEnvironment([ "CONFIGDIR": "/home/other/conf" ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showConfPath').outcome == SUCCESS
        result3.output.contains("/home/other/conf/cluster.properties")

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'check simple cartridge list and setup cartridges'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultECL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_PROD, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultECL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_PROD}").outcome == SUCCESS

        when:
        def resultDEVECL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultDEVECL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST}").outcome == SUCCESS

        when:
        def resultTESTECL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_TEST, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultTESTECL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_TEST}").outcome == SUCCESS

        when:
        def resultSC = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_PROD, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultSC.task(":${com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_PROD}").outcome == SUCCESS

        when:
        def resultDEVSC = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultDEVSC.task(":${com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES}").outcome == SUCCESS

        when:
        def resultTESTSC = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_TEST, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultTESTSC.task(":${com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_TEST}").outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'copy 3rd party libs'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def result3RDPL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_PROD, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3RDPL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_PROD}").outcome == SUCCESS

        when:
        def result3RDPLDev = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3RDPLDev.task(":${com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS}").outcome == SUCCESS

        when:
        def result3RDPLTest = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_TEST, "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3RDPLTest.task(":${com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_TEST}").outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareDefaultBuildConfig(File testProjectDir, File settingsFile, File buildFile) {
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
                    
                    cartridges = [ 'com.intershop.cartridge:cartridge_test:1.0.0', 
                                   'testCartridge1',
                                   'com.intershop.cartridge:cartridge_dev:1.0.0', 
                                   'com.intershop.cartridge:cartridge_adapter:1.0.0',
                                   'testCartridge4',
                                   'testCartridge3',
                                   'testCartridge2',
                                   'com.intershop.cartridge:cartridge_prod:1.0.0' ] 

                    dbprepareCartridges = [ 'testCartridge1',
                                            'testCartridge2' ] 

                    base {
                        dependency = "com.intershop.icm:icm-as:1.0.0"
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.product'
        }
        
        dependencies {
            implementation 'com.google.inject:guice:4.0'
            implementation 'com.google.inject.extensions:guice-servlet:3.0'
            implementation 'javax.servlet:javax.servlet-api:3.1.0'
        
            implementation 'io.prometheus:simpleclient:0.6.0'
            implementation 'io.prometheus:simpleclient_hotspot:0.6.0'
            implementation 'io.prometheus:simpleclient_servlet:0.6.0'
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.test'
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

        def prj3dir = createSubProject('testCartridge3', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.development'
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('testCartridge4', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.adapter'
        }
        
        dependencies {
            implementation 'ch.qos.logback:logback-core:1.2.3'
            implementation 'ch.qos.logback:logback-classic:1.2.3'
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        writeJavaTestClass("com.intershop.testCartridge1", prj1dir)
        writeJavaTestClass("com.intershop.testCartridge2", prj2dir)
        writeJavaTestClass("com.intershop.testCartridge3", prj3dir)
        writeJavaTestClass("com.intershop.testCartridge4", prj4dir)

        return repoConf
    }

    def "simple test of tasks for folder creation"() {
        prepareExtendedBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultDevConf = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultDevConf.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER}").outcome == SUCCESS

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_PROD}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultProdConf.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_PROD}").outcome == SUCCESS

        when:
        def resultTestConf = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_TEST}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultTestConf.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_TEST}").outcome == SUCCESS

        when:
        def resultDevSites = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultDevSites.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER}").outcome == SUCCESS

        when:
        def resultProdSites = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_PROD}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultProdSites.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_PROD}").outcome == SUCCESS

        when:
        def resultTestSites = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_TEST}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultTestSites.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_TEST}").outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareExtendedBuildConfig(File testProjectDir, File settingsFile, File buildFile) {
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

        def testFile2 = new File(testProjectDir, "sites/base/test-site1/units/test.properties")
        testFile2.parentFile.mkdirs()
        testFile2 << "sites = 1"

        def testFile3 = new File(testProjectDir, "sites/base/test-site2/units/test.properties")
        testFile3.parentFile.mkdirs()
        testFile3 << "sites = 2"

        def testFile_test1 = new File(testProjectDir, "config/test/cluster/test.properties")
        testFile_test1.parentFile.mkdirs()
        testFile_test1 << "test_test = 1"

        def testFile1_test1 = new File(testProjectDir, "config/dev/cluster/test.properties")
        testFile1_test1.parentFile.mkdirs()
        testFile1_test1 << "dev_test = 2"

        def testFile2_test1 = new File(testProjectDir, "sites/test/test-site1/units/test.properties")
        testFile2_test1.parentFile.mkdirs()
        testFile2_test1 << "test_sites = 1"

        def testFile3_test1 = new File(testProjectDir, "sites/dev/test-site1/units/test.properties")
        testFile3_test1.parentFile.mkdirs()
        testFile3_test1 << "dev_sites = 2"

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.project'
            }
            
            group = 'com.intershop.test'

            intershop {
                projectConfig {
                    
                    cartridges = [ 'com.intershop.cartridge:cartridge_test:1.0.0', 
                                   'testCartridge1',
                                   'com.intershop.cartridge:cartridge_dev:1.0.0', 
                                   'com.intershop.cartridge:cartridge_adapter:1.0.0',
                                   'testCartridge4',
                                   'testCartridge3',
                                   'testCartridge2',
                                   'com.intershop.cartridge:cartridge_prod:1.0.0' ] 

                    dbprepareCartridges = [ 'testCartridge1',
                                            'testCartridge2' ] 

                    base {
                        dependency = "com.intershop.icm:icm-as:1.0.0"
                    }

                    modules {
                        solrExt {
                            dependency = "com.intershop.search:solrcloud:1.0.0"
                            configPackage {
                                exclude("**/cluster/version.properties")
                            }
                        }
                        paymentExt {
                            dependency = "com.intershop.payment:paymenttest:1.0.0"
                            configPackage {
                                exclude("**/cluster/version.properties")
                            }
                        }
                    }

                    serverDirConfig {
                        base {
                            sites {
                                dirs {
                                    main {
                                        dir.set(file("sites/base"))
                                        exclude("**/units/test.properties")
                                    }
                                }
                            }
                            config {
                                dirs {
                                    main {
                                        dir.set(file("config/base"))
                                        exclude("**/cluster/test.properties")
                                    }
                                }
                                exclude("**/cluster/cartridgelist.properties")
                            }
                        }
                        test {
                            sites {
                                dirs {
                                    main {
                                        dir.set(file("sites/test"))
                                    }
                                }
                            }
                            config {
                                dirs {
                                    main {
                                        dir.set(file("config/test"))
                                    }
                                }
                            }
                        }
                        dev {
                            sites {
                                dirs {
                                    main {
                                        dir.set(file("sites/dev"))
                                    }
                                    test {
                                        dir.set(file("sites/test"))
                                        exclude("**/units/test.properties")
                                    }
                                }
                            }
                            config {
                                dirs {
                                    main {
                                        dir.set(file("config/dev"))
                                    }
                                    test {
                                        dir.set(file("config/test"))
                                        exclude("**/cluster/test.properties")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.product'
        }
        
        dependencies {
            implementation 'com.google.inject:guice:4.0'
            implementation 'com.google.inject.extensions:guice-servlet:3.0'
            implementation 'javax.servlet:javax.servlet-api:3.1.0'
        
            implementation 'io.prometheus:simpleclient:0.6.0'
            implementation 'io.prometheus:simpleclient_hotspot:0.6.0'
            implementation 'io.prometheus:simpleclient_servlet:0.6.0'
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.test'
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

        def prj3dir = createSubProject('testCartridge3', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.development'
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('testCartridge4', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.adapter'
        }
        
        dependencies {
            implementation 'ch.qos.logback:logback-core:1.2.3'
            implementation 'ch.qos.logback:logback-classic:1.2.3'
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        writeJavaTestClass("com.intershop.testCartridge1", prj1dir)
        writeJavaTestClass("com.intershop.testCartridge2", prj2dir)
        writeJavaTestClass("com.intershop.testCartridge3", prj3dir)
        writeJavaTestClass("com.intershop.testCartridge4", prj4dir)

        return repoConf
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
            id 'com.intershop.icm.cartridge.product'
        }
        
        dependencies {
            implementation 'com.google.inject:guice:4.0'
            implementation 'com.google.inject.extensions:guice-servlet:3.0'
            implementation 'javax.servlet:javax.servlet-api:3.1.0'
        
            implementation 'io.prometheus:simpleclient:0.6.0'
            implementation 'io.prometheus:simpleclient_hotspot:0.6.0'
            implementation 'io.prometheus:simpleclient_servlet:0.6.0'
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.product'
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

    def "prepare folder for publishing without libs.txt"() {
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
                            dependency = "com.intershop.icm:icm-as:2.0.0"
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

    def "prepare folder for publishing without sites"() {
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
                            dependency = "com.intershop.icm:icm-as:3.0.0"
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

    def "prepare folder for publishing without anything"() {
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
                            dependency = "com.intershop.icm:icm-as:4.0.0"
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

    def "prepare folder with sites"() {
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

        def testFile2 = new File(testProjectDir, "sites/base/test-site1/units/test.properties")
        testFile2.parentFile.mkdirs()
        testFile2 << "sites = 1"

        def testFile3 = new File(testProjectDir, "sites/base/test-site2/units/test.properties")
        testFile3.parentFile.mkdirs()
        testFile3 << "sites = 2"

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

                    sites {
                        dir("sites/base")
                        targetPath = "sites"
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

        then:
        result.task(':prepareServer').outcome == SUCCESS
        (new File(testProjectDir, "target/server/sites/sites/test-site1/units/test.properties")).exists()
        (new File(testProjectDir, "target/server/sites/sites/test-site2/units/test.properties")).exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    def "prepare folder with configs"() {
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

        def testFile2 = new File(testProjectDir, "sites/base/test-site1/units/test.properties")
        testFile2.parentFile.mkdirs()
        testFile2 << "sites = 1"

        def testFile3 = new File(testProjectDir, "sites/base/test-site2/units/test.properties")
        testFile3.parentFile.mkdirs()
        testFile3 << "sites = 2"

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
                        solr {
                            dependency = "com.intershop.icm:solrcloud:1.0.0"
                        }
                    }

                    conf {
                        dir("config/base")
                        targetPath = "system-conf"
                    }

                    sites {
                        dir("sites/base")
                        targetPath = "sites"
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

        then:
        result.task(':prepareServer').outcome == SUCCESS
        (new File(testProjectDir, "target/server/sites/sites/test-site1/units/test.properties")).exists()
        (new File(testProjectDir, "target/server/sites/sites/test-site2/units/test.properties")).exists()

        where:
        gradleVersion << supportedGradleVersions
    }
}
