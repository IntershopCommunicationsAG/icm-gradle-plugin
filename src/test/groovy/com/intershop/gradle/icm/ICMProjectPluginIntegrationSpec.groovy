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

import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.util.TestRepo
import com.intershop.gradle.test.AbstractIntegrationGroovySpec

import java.util.zip.ZipFile

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
                .withArguments("showLicPath", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':showLicPath').outcome == SUCCESS
        result.output.contains(new File("gradle/icm-default/lic/license.xml").toString())

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s", "--warning-mode", "all", "-DlicenseDir=/home/user/licdir")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showLicPath').outcome == SUCCESS
        result1.output.contains(new File("/home/user/licdir/license.xml").toString())

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s", "--warning-mode", "all", "-PlicenseDir=/home/otheruser/licdir")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showLicPath').outcome == SUCCESS
        result2.output.contains(new File("/home/otheruser/licdir/license.xml").toString())

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s")
                .withEnvironment([ "LICENSEDIR": "/home/other/licdir" ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showLicPath').outcome == SUCCESS
        result3.output.contains(new File("/home/other/licdir/license.xml").toString())

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
                .withArguments("showConfPath", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':showConfPath').outcome == SUCCESS
        result.output.contains(new File("gradle/icm-default/conf/icm.properties").toString())

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-DconfigDir=/home/user/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showConfPath').outcome == SUCCESS
        result1.output.contains(new File("/home/user/conf/icm.properties").toString())

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-PconfigDir=/home/otheruser/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showConfPath').outcome == SUCCESS
        result2.output.contains(new File("/home/otheruser/conf/icm.properties").toString())

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all")
                .withEnvironment([ "CONFIGDIR": "/home/other/conf" ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showConfPath').outcome == SUCCESS
        result3.output.contains(new File("/home/other/conf/icm.properties").toString())

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'collect libraries'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def result3RDPL = getPreparedGradleRunner()
                .withArguments("collectLibraries", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def prodLibDir = new File(testProjectDir, "build/libraries/production")
        def testLibDir = new File(testProjectDir, "build/libraries/test")

        then:
        result3RDPL.task(":collectLibraries").outcome == SUCCESS
        prodLibDir.exists()
        prodLibDir.listFiles()?.size() == 13
        new File(prodLibDir, "aopalliance_aopalliance_1.0.jar").exists()
        new File(prodLibDir, "ch.qos.logback_logback-classic_1.2.3.jar").exists()
        new File(prodLibDir, "ch.qos.logback_logback-core_1.2.3.jar").exists()
        new File(prodLibDir, "com.google.guava_guava_16.0.1.jar").exists()
        new File(prodLibDir, "com.google.inject.extensions_guice-servlet_3.0.jar").exists()
        new File(prodLibDir, "com.google.inject_guice_4.0.jar").exists()
        new File(prodLibDir, "io.prometheus_simpleclient_0.6.0.jar").exists()
        new File(prodLibDir, "io.prometheus_simpleclient_common_0.6.0.jar").exists()
        new File(prodLibDir, "io.prometheus_simpleclient_hotspot_0.6.0.jar").exists()
        new File(prodLibDir, "io.prometheus_simpleclient_servlet_0.6.0.jar").exists()
        new File(prodLibDir, "javax.inject_javax.inject_1.jar").exists()
        new File(prodLibDir, "javax.servlet_javax.servlet-api_3.1.0.jar").exists()
        new File(prodLibDir, "org.slf4j_slf4j-api_1.7.25.jar").exists()

        testLibDir.exists()
        testLibDir.listFiles()?.size() == 3
        new File(testLibDir, "commons-collections_commons-collections_3.2.2.jar").exists()
        new File(testLibDir, "org.codehaus.janino_commons-compiler_3.0.6.jar").exists()
        new File(testLibDir, "org.codehaus.janino_janino_2.5.16.jar").exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    private def createLocalFile(String path, String content) {
        def testFile = new File(testProjectDir, path)
        testFile.parentFile.mkdirs()
        testFile << content.stripIndent()
    }

    private def prepareDefaultBuildConfig(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.project'
            }
            
            group = 'com.intershop.test'
            version = '10.0.0'

            intershop {
                projectConfig {

                    base {
                        dependency = "com.intershop.icm:icm-as:1.0.0"
                        platforms = [ "com.intershop:libbom:1.0.0" ]
                        image.set("intershophub/icm-as:11.0.1")
                    }

                    modules {
                        solrExt {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")
                        }
                        paymentExt {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")
                        }
                    }
                }
            }

            ${repoConf}

        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
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

        def prj2dir = createSubProject('prjCartridge_test', """
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

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.development'
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
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

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        return repoConf
    }

    def "test of prod tasks for config folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        buildFile << """

            plugins.withType(com.intershop.gradle.icm.ICMProjectPlugin) {
                task("printProdFolder") {
                    doLast {
                        println("ProdFolder: \${project.extensions.getByName("intershop").projectConfig.containerConfig}")
                    }
                }
            }
        """.stripIndent()

        when:
        def resultPrintProdConfFolder = getPreparedGradleRunner()
                .withArguments("printProdFolder", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPrintProdConfFolder.task(":printProdFolder").outcome == SUCCESS
        resultPrintProdConfFolder.output.contains(new File("build/container/config_folder").toString())

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("createConfigProd", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def versionFile = new File(testProjectDir,"build/container/config_folder/system-conf/cluster/version.properties")

        then:
        resultProdConf.task(":createConfigProd").outcome == SUCCESS

        versionFile.exists()
        versionFile.text.contains("version.information.version=10.0.0")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test of test tasks for config folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        buildFile << """

            plugins.withType(com.intershop.gradle.icm.ICMProjectPlugin) {
                task("printTestFolder") {
                    doLast {
                        println("ProdFolder: \${project.extensions.getByName("intershop").projectConfig.testcontainerConfig}")
                    }
                }
            }
        """.stripIndent()

        when:
        def resultPrintProdConfFolder = getPreparedGradleRunner()
                .withArguments("printTestFolder", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPrintProdConfFolder.task(":printTestFolder").outcome == SUCCESS
        resultPrintProdConfFolder.output.contains(new File("build/testcontainer/config_folder").toString())

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("createConfigTest", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def versionFile = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/cluster/version.properties")

        then:
        resultProdConf.task(":createConfigTest").outcome == SUCCESS

        versionFile.exists()
        versionFile.text.contains("version.information.version=10.0.0")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test of dev tasks for config folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        buildFile << """

            plugins.withType(com.intershop.gradle.icm.ICMProjectPlugin) {
                task("printDevFolder") {
                    doLast {
                        println("ProdFolder: \${project.extensions.getByName("intershop").projectConfig.config}")
                    }
                }
            }
        """.stripIndent()

        when:
        def resultPrintProdConfFolder = getPreparedGradleRunner()
                .withArguments("printDevFolder", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPrintProdConfFolder.task(":printDevFolder").outcome == SUCCESS
        resultPrintProdConfFolder.output.contains(new File( "build/server/config_folder").toString())

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("createConfig", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def versionFile = new File(testProjectDir,"build/server/config_folder/system-conf/cluster/version.properties")

        then:
        resultProdConf.task(":createConfig").outcome == SUCCESS

        versionFile.exists()
        versionFile.text.contains("version.information.version=10.0.0")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "prepare folder for release"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareContainer", "-s")
                .withGradleVersion(gradleVersion)
                .build()


        def configDir = new File(testProjectDir, "build/container/config_folder/system-conf" )
        def configClusterDir = new File(configDir, "cluster")
        def prodLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        configDir.exists()
        configDir.listFiles().size() == 1
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 1
        prodLibsDir.exists()
        prodLibsDir.listFiles()?.size() == 13
        testLibsDir.exists()
        testLibsDir.listFiles()?.size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    def "prepare folder for development"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareServer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def configDir = new File(testProjectDir, "build/server/config_folder/system-conf" )
        def configClusterDir = new File(configDir, "cluster")
        def prodLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareServer').outcome == SUCCESS
        configDir.exists()
        configDir.listFiles().size() == 1
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 1
        prodLibsDir.exists()
        prodLibsDir.listFiles()?.size() == 13
        testLibsDir.exists()
        testLibsDir.listFiles()?.size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareDefaultBuildConfigwithPublishing(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.project'
                id 'maven-publish'
            }
            
            group = 'com.intershop.test'
            version = '10.0.0'

            intershop {
                projectConfig {
                    base {
                        dependency = "com.intershop.icm:icm-as:1.0.0"
                        platforms = [ "com.intershop:libbom:1.0.0" ]
                        image.set("intershophub/icm-as:11.0.1")
                    }

                    modules {
                        solrExt {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")
                        }
                        paymentExt {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")
                        }
                    }
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = "\$buildDir/pubrepo"
                    }
                }
            }
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
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

        def prj2dir = createSubProject('prjCartridge_test', """
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

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.development'
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
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

        def prj5dir = createSubProject('prjCartridge_testext', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.test'
            id 'com.intershop.icm.cartridge.external'
        }
        
        group = 'com.intershop.test'
        version = '10.0.0'

        dependencies {
            cartridge 'com.intershop.cartridge:cartridge_adapter:1.0.0' 
        }

        ${repoConf}
   
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = "\${rootProject.buildDir}/pubrepo"
                }
            }
        }  
        """.stripIndent())

        def prj6dir = createSubProject('prjCartridge_static', """
        plugins {
            id 'com.intershop.icm.cartridge.adapter'
        }
        
        group = 'com.intershop.test'
        version = '10.0.0'

        dependencies {
            cartridge 'com.intershop.cartridge:cartridge_adapter:1.0.0' 
        }

         ${repoConf}
    
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = "\${rootProject.buildDir}/pubrepo"
                }
            }
        }  
        """.stripIndent())

        file("staticfiles/cartridge/logback/logback-test.xml", prj6dir)
        file("staticfiles/cartridge/components/cartridge.components", prj6dir)

        def prj7dir = createSubProject('prjCartridge_container', """
        plugins {
            id 'java'
            id 'com.intershop.icm.cartridge.container'
        }
        
        group = 'com.intershop.test'
        version = '10.0.0'

        dependencies {
            cartridge 'com.intershop.cartridge:cartridge_adapter:1.0.0' 
        }

         ${repoConf}
    
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = "\${rootProject.buildDir}/pubrepo"
                }
            }
        }  
        """.stripIndent())

        file("staticfiles/cartridge/logback/logback2-test.xml", prj7dir)
        file("staticfiles/cartridge/components/cartridge2.components", prj7dir)

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)
        writeJavaTestClass("com.intershop.testext", prj5dir)
        writeJavaTestClass("com.intershop.container", prj7dir)

        return repoConf
    }

    private def prepareDefaultBuildConfigWithoutLibsTxt(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/base/cluster/cartridgelist.properties", "cartridgelist = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.project'
                id 'maven-publish'
            }
            
            group = 'com.intershop.test'
            version = '10.0.0'

            intershop {
                projectConfig {
                    base {
                        dependency = "com.intershop.icm:icm-as:2.0.0"
                        image.set("intershophub/icm-as:11.0.1")
                    }

                    modules {
                        solrExt {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")
                        }
                        paymentExt {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")
                        }
                    }
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = "\$buildDir/pubrepo"
                    }
                }
            }
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
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

        def prj2dir = createSubProject('prjCartridge_test', """
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

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge.development'
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
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

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        return repoConf
    }

    def "prepare folder without libs.txt"() {
        prepareDefaultBuildConfigWithoutLibsTxt(testProjectDir, settingsFile, buildFile)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareContainer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def configDir = new File(testProjectDir, "build/container/config_folder/system-conf" )
        def configClusterDir = new File(configDir, "cluster")
        def prodLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        configDir.exists()
        configDir.listFiles().size() == 1
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 1
        prodLibsDir.exists()
        prodLibsDir.listFiles()?.size() == 13
        testLibsDir.exists()
        testLibsDir.listFiles()?.size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }
    
    def "Create list from Dependency"() {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoConfig()

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.project'
            }
            
            task getList(type: com.intershop.gradle.icm.tasks.GetDependencyList) { 
                dependency = "com.intershop:libbom:1.0.0"
            }
            
            ${repoConf}
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("getList", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':getList').outcome == SUCCESS
        result.output.contains("com.other:library1:1.5.0")
        result.output.contains("com.other:library2:1.5.0")
        result.output.contains("com.other:library3:1.5.0")

        where:
        gradleVersion << supportedGradleVersions
    }
}
