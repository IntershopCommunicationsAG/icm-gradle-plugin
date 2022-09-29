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

import java.util.zip.ZipFile

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ICMProjectPluginIntegrationKotlinSpec extends AbstractIntegrationKotlinSpec {

    def 'check lic configuration from extension'() {
        given:
        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.base")
            }
            
            val sfile = File(project.gradle.gradleUserHomeDir, "icm-default/lic/license.xml")
            if(! sfile.exists()) {
                sfile.parentFile.mkdirs()
                sfile.createNewFile()
            }
            
            tasks.register("showLicPath") { 
                doLast {
                    val extension = project.extensions.getByName("intershop") as com.intershop.gradle.icm.extension.IntershopExtension
                    println(extension.developmentConfig.licenseFilePath)
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
        result.output.contains(".gradle/icm-default/lic/license.xml")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s", "--warning-mode", "all", "-DlicenseDir=/home/user/licdir")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showLicPath').outcome == SUCCESS
        result1.output.contains("/home/user/licdir/license.xml")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showLicPath", "-s", "--warning-mode", "all", "-PlicenseDir=/home/otheruser/licdir")
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
        rootProject.name="rootproject"
        """.stripIndent()

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.base")
            }
            
            tasks.register("showConfPath") { 
                doLast {
                    val extension = project.extensions.getByName("intershop") as com.intershop.gradle.icm.extension.IntershopExtension
                    println(extension.developmentConfig.configFilePath)
                }
            }
            """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.output.contains(".gradle/icm-default/conf/icm.properties")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-DconfigDir=/home/user/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showConfPath').outcome == SUCCESS
        result1.output.contains("/home/user/conf/icm.properties")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-PconfigDir=/home/otheruser/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showConfPath').outcome == SUCCESS
        result2.output.contains("/home/otheruser/conf/icm.properties")

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all")
                .withEnvironment([ "CONFIGDIR": "/home/other/conf" ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showConfPath').outcome == SUCCESS
        result3.output.contains("/home/other/conf/icm.properties")

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
        String repoConf = repo.getRepoKtsConfig()

        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
            }

            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                        platform("com.intershop:libbom:1.0.0")
                        image.set("intershophub/icm-as:11.0.1")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")
                        }
                    }

                    serverDirConfig {
                        base {
                            dirs {
                                named("main") {
                                    dir.set(file("config/base"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                            exclude("**/cluster/cartridgelist.properties")
                        }
                        prod {
                            dirs {
                                register("main") {
                                    dir.set(file("config/prod"))
                                }
                            }
                        }
                        test {
                            dirs {
                                register("main") {
                                    dir.set(file("config/test"))
                                }
                            }
                        }
                        dev {
                            dirs {
                                register("main") {
                                    dir.set(file("config/dev"))
                                }
                                register("test") {
                                    dir.set(file("config/test"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                        }
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.product")
        }
        
        dependencies {
            implementation( "com.google.inject:guice:4.0" )
            implementation( "com.google.inject.extensions:guice-servlet:3.0" )
            implementation( "javax.servlet:javax.servlet-api:3.1.0" )
        
            implementation( "io.prometheus:simpleclient:0.6.0" )
            implementation( "io.prometheus:simpleclient_hotspot:0.6.0" )
            implementation( "io.prometheus:simpleclient_servlet:0.6.0" )
        } 
        
        repositories {
            mavenCentral()
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
            `java`
            id("com.intershop.icm.cartridge.test")
        }
        
        dependencies {
            implementation("org.codehaus.janino:janino:2.5.16")
            implementation("org.codehaus.janino:commons-compiler:3.0.6")
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("commons-collections:commons-collections:3.2.2")
        } 
        
        repositories {
            mavenCentral()
        }        
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            mavenCentral()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
        plugins {
            `java`
            id("com.intershop.icm.cartridge.adapter")
        }
        
        dependencies {
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
        } 
        
        repositories {
            mavenCentral()
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

            plugins.withType(com.intershop.gradle.icm.ICMProjectPlugin::class.java) {
                tasks.register("printProdFolder") { 
                    doLast {
                        val extension = project.extensions.getByName("intershop") as com.intershop.gradle.icm.extension.IntershopExtension
                        println(extension.projectConfig.containerConfig)
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
        resultPrintProdConfFolder.output.contains("build/container/config_folder")

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("createConfigProd", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def versionFile = new File(testProjectDir,"build/container/config_folder/system-conf/cluster/version.properties")
        def apps1File = new File(testProjectDir,"build/container/config_folder/system-conf/apps/file.txt")
        def apps2File = new File(testProjectDir,"build/container/config_folder/system-conf/apps/file2.txt")
        def apps3File = new File(testProjectDir,"build/container/config_folder/system-conf/apps/paymenr.txt")
        def testPropsFile = new File(testProjectDir,"build/container/config_folder/system-conf/cluster/test.properties")

        then:
        resultProdConf.task(":createConfigProd").outcome == SUCCESS

        versionFile.exists()
        versionFile.text.contains("version.information.version=10.0.0")
        apps1File.exists()
        apps1File.text.contains("apps = test1.component(com.intershop.icm:icm-as:1.0.0)")
        apps2File.exists()
        apps2File.text.contains("apps = test2.component (com.intershop.search:solrcloud:1.0.0)")
        apps3File.exists()
        apps3File.text.contains("payment = test2.component")
        testPropsFile.exists()
        testPropsFile.text.contains("test.properties = prod_dir")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test of test tasks for config folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        buildFile << """

            plugins.withType(com.intershop.gradle.icm.ICMProjectPlugin::class.java) {
                tasks.register("printTestFolder") { 
                    doLast {
                        val extension = project.extensions.getByName("intershop") as com.intershop.gradle.icm.extension.IntershopExtension
                        println(extension.projectConfig.testcontainerConfig)
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
        resultPrintProdConfFolder.output.contains("build/testcontainer/config_folder")

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("createConfigTest", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def versionFile = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/cluster/version.properties")
        def apps1File = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/apps/file.txt")
        def apps2File = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/apps/file2.txt")
        def apps3File = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/apps/paymenr.txt")
        def testPropsFile = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/cluster/test.properties")

        then:
        resultProdConf.task(":createConfigTest").outcome == SUCCESS

        versionFile.exists()
        versionFile.text.contains("version.information.version=10.0.0")
        apps1File.exists()
        apps1File.text.contains("apps = test1.component(com.intershop.icm:icm-as:1.0.0)")
        apps2File.exists()
        apps2File.text.contains("apps = test2.component (com.intershop.search:solrcloud:1.0.0)")
        apps3File.exists()
        apps3File.text.contains("payment = test2.component")
        testPropsFile.exists()
        testPropsFile.text.contains("test_test = 1")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test of dev tasks for config folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        buildFile << """

            plugins.withType(com.intershop.gradle.icm.ICMProjectPlugin::class.java) {
                tasks.register("printDevFolder") { 
                    doLast {
                        val extension = project.extensions.getByName("intershop") as com.intershop.gradle.icm.extension.IntershopExtension
                        println(extension.projectConfig.config)
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
        resultPrintProdConfFolder.output.contains("build/server/config_folder")

        when:
        def resultProdConf = getPreparedGradleRunner()
                .withArguments("createConfig", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def versionFile = new File(testProjectDir,"build/server/config_folder/system-conf/cluster/version.properties")
        def apps1File = new File(testProjectDir,"build/server/config_folder/system-conf/apps/file.txt")
        def apps2File = new File(testProjectDir,"build/server/config_folder/system-conf/apps/file2.txt")
        def apps3File = new File(testProjectDir,"build/server/config_folder/system-conf/apps/paymenr.txt")
        def testPropsFile = new File(testProjectDir,"build/server/config_folder/system-conf/cluster/test.properties")

        then:
        resultProdConf.task(":createConfig").outcome == SUCCESS

        versionFile.exists()
        versionFile.text.contains("version.information.version=10.0.0")
        apps1File.exists()
        apps1File.text.contains("apps = test1.component(com.intershop.icm:icm-as:1.0.0)")
        apps2File.exists()
        apps2File.text.contains("apps = test2.component (com.intershop.search:solrcloud:1.0.0)")
        apps3File.exists()
        apps3File.text.contains("payment = test2.component")
        testPropsFile.exists()
        testPropsFile.text.contains("dev_test = 1")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test cartridge.properties from dependency"() {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
            }
            
            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {
                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                        platform("com.intershop:libbom:1.0.0")
                        image.set("intershophub/icm-as:11.0.1")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")
                        }
                    }
                }
            }

            ${repoConf}
        """.stripIndent()

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
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        configDir.exists()
        configDir.listFiles().size() == 2
        configAppsDir.exists()
        configAppsDir.listFiles().size() == 3
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 2
        testLibsDir.exists()
        testLibsDir.listFiles().size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    def "prepare folder for developement"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("prepareServer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridgesDir = new File(testProjectDir, "build/server/cartridges")
        def configDir = new File(testProjectDir, "build/server/config_folder/system-conf" )
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def productionLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareServer').outcome == SUCCESS
        configDir.exists()
        configDir.listFiles().size() == 2
        configAppsDir.exists()
        configAppsDir.listFiles().size() == 3
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 2
        productionLibsDir.exists()
        productionLibsDir.listFiles().size() == 13
        testLibsDir.exists()
        testLibsDir.listFiles().size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareDefaultBuildConfigwithPublishing(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/base/cluster/cartridgelist.properties", "cartridgelist = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
                `maven-publish`
            }
            
            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                        platform("com.intershop:libbom:1.0.0")
                        image.set("intershophub/icm-as:11.0.1")                        
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")
                        }
                    }

                    serverDirConfig {
                        base {
                            dirs {
                                named("main") {
                                    dir.set(file("config/base"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                            exclude("**/cluster/cartridgelist.properties")
                        }
                        prod {
                            dirs {
                                register("main") {
                                    dir.set(file("config/prod"))
                                }
                            }
                        }
                        test {
                            dirs {
                                register("main") {
                                    dir.set(file("config/test"))
                                }
                            }
                        }
                        dev {
                            dirs {
                                register("main") {
                                    dir.set(file("config/dev"))
                                }
                                register("test") {
                                    dir.set(file("config/test"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                        }
                    }
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri("\$buildDir/pubrepo")
                    }
                }
            }
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
        plugins {
            `java`
            id("com.intershop.icm.cartridge.product")
        }
        
        dependencies {
            implementation("com.google.inject:guice:4.0")
            implementation("com.google.inject.extensions:guice-servlet:3.0")
            implementation("javax.servlet:javax.servlet-api:3.1.0")
        
            implementation("io.prometheus:simpleclient:0.6.0")
            implementation("io.prometheus:simpleclient_hotspot:0.6.0")
            implementation("io.prometheus:simpleclient_servlet:0.6.0")
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
            `java`
            id("com.intershop.icm.cartridge.test")
        }
        
        dependencies {
            implementation("org.codehaus.janino:janino:2.5.16")
            implementation("org.codehaus.janino:commons-compiler:3.0.6")
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("commons-collections:commons-collections:3.2.2")
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.adapter")
        }
        
        dependencies {
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
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

    def "prepare folder for publishing"() {
        prepareDefaultBuildConfigwithPublishing(testProjectDir, settingsFile, buildFile)
        when:
        def result = getPreparedGradleRunner()
                .withArguments("publish", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def repoConfigFile = new File(testProjectDir, "build/pubrepo/com/intershop/test/rootproject/10.0.0/rootproject-10.0.0-configuration.zip")

        then:
        result.task(':publish').outcome == SUCCESS
        result.task(':zipConfiguration').outcome == SUCCESS
        repoConfigFile.exists()
        new ZipFile(repoConfigFile).entries().toList().size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareDefaultBuildConfigWithoutLibsTxt(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/base/cluster/cartridgelist.properties", "cartridgelist = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
                `maven-publish`
            }
            
            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {
                    base {
                        dependency.set("com.intershop.icm:icm-as:2.0.0")
                        image.set("intershophub/icm-as:11.0.1")  
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")  
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")  
                        }
                    }

                    serverDirConfig {
                        base {
                            dirs {
                                named("main") {
                                    dir.set(file("config/base"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                            exclude("**/cluster/cartridgelist.properties")
                        }
                        prod {
                            dirs {
                                register("main") {
                                    dir.set(file("config/prod"))
                                }
                            }
                        }
                        test {
                            dirs {
                                register("main") {
                                    dir.set(file("config/test"))
                                }
                            }
                        }
                        dev {
                            dirs {
                                register("main") {
                                    dir.set(file("config/dev"))
                                }
                                register("test") {
                                    dir.set(file("config/test"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                        }
                    }
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri("\$buildDir/pubrepo")
                    }
                }
            }
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.product")
        }
        
        dependencies {
            implementation("com.google.inject:guice:4.0")
            implementation("com.google.inject.extensions:guice-servlet:3.0")
            implementation("javax.servlet:javax.servlet-api:3.1.0")
        
            implementation("io.prometheus:simpleclient:0.6.0")
            implementation("io.prometheus:simpleclient_hotspot:0.6.0")
            implementation("io.prometheus:simpleclient_servlet:0.6.0")
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.test")
        }
        
        dependencies {
            implementation("org.codehaus.janino:janino:2.5.16")
            implementation("org.codehaus.janino:commons-compiler:3.0.6")
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("commons-collections:commons-collections:3.2.2")
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.adapter")
        }
        
        dependencies {
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
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
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def productionLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        configDir.exists()
        configDir.listFiles().size() == 2
        configAppsDir.exists()
        configAppsDir.listFiles().size() == 3
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 2
        productionLibsDir.exists()
        productionLibsDir.listFiles().size() == 13
        testLibsDir.exists()
        testLibsDir.listFiles().size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareBuildConfigwithPublishingWithoutSites(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/base/cluster/cartridgelist.properties", "cartridgelist = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        createLocalFile("sites/base/.empty", "do not delete")

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
                `maven-publish`
            }
            
            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {                   
                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                        image.set("intershophub/icm-as:11.0.1")  
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                            image.set("intershophub/solrcloud:11.0.1")  
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                            image.set("intershophub/paymenttest:11.0.1")  
                        }
                    }

                    serverDirConfig {
                        base {
                            dirs {
                                named("main") {
                                    dir.set(file("config/base"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                            exclude("**/cluster/cartridgelist.properties")
                        }
                        prod {
                            dirs {
                                register("main") {
                                    dir.set(file("config/prod"))
                                }
                            }
                        }
                        test {
                            dirs {
                                register("main") {
                                    dir.set(file("config/test"))
                                }
                            }
                        }
                        dev {
                            dirs {
                                register("main") {
                                    dir.set(file("config/dev"))
                                }
                                register("test") {
                                    dir.set(file("config/test"))
                                    exclude("**/cluster/test.properties")
                                }
                            }
                        }
                    }
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri("\$buildDir/pubrepo")
                    }
                }
            }
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.product")
        }
        
        dependencies {
            implementation("com.google.inject:guice:4.0")
            implementation("com.google.inject.extensions:guice-servlet:3.0")
            implementation("javax.servlet:javax.servlet-api:3.1.0")
        
            implementation("io.prometheus:simpleclient:0.6.0")
            implementation("io.prometheus:simpleclient_hotspot:0.6.0")
            implementation("io.prometheus:simpleclient_servlet:0.6.0")
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.test")
        }
        
        dependencies {
            implementation("org.codehaus.janino:janino:2.5.16")
            implementation("org.codehaus.janino:commons-compiler:3.0.6")
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("commons-collections:commons-collections:3.2.2")
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.adapter")
        }
        
        dependencies {
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
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

    def "prepare folder for publishing without sites"() {
        prepareBuildConfigwithPublishingWithoutSites(testProjectDir, settingsFile, buildFile)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("publish", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def repoConfigFile = new File(testProjectDir, "build/pubrepo/com/intershop/test/rootproject/10.0.0/rootproject-10.0.0-configuration.zip")
        then:
        result.task(':publish').outcome == SUCCESS
        result.task(':zipConfiguration').outcome == SUCCESS
        repoConfigFile.exists()
        new ZipFile(repoConfigFile).entries().toList().size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    private def prepareBuildConfigwithPublishingWithoutAnything(File testProjectDir, File settingsFile, File buildFile) {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        settingsFile << """
        rootProject.name="rootproject"
        """.stripIndent()

        createLocalFile("sites/base/.empty", "do not delete")
        createLocalFile("config/base/.empty", "do not delete")

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
                `maven-publish`
            }
            
            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {
                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                        platform("com.intershop:libbom:1.0.0")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                        }
                    }
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri("\$buildDir/pubrepo")
                    }
                }
            }
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.product")
        }
        
        dependencies {
            implementation("com.google.inject:guice:4.0")
            implementation("com.google.inject.extensions:guice-servlet:3.0")
            implementation("javax.servlet:javax.servlet-api:3.1.0")
        
            implementation("io.prometheus:simpleclient:0.6.0")
            implementation("io.prometheus:simpleclient_hotspot:0.6.0")
            implementation("io.prometheus:simpleclient_servlet:0.6.0")
        } 
        
        repositories {
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.test")
        }
        
        dependencies {
            implementation("org.codehaus.janino:janino:2.5.16")
            implementation("org.codehaus.janino:commons-compiler:3.0.6")
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("commons-collections:commons-collections:3.2.2")
        } 
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            jcenter()
        }        
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.adapter")
        }
        
        dependencies {
            implementation("ch.qos.logback:logback-core:1.2.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
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

    def "prepare folder for publishing without anything"() {
        prepareBuildConfigwithPublishingWithoutAnything(testProjectDir, settingsFile, buildFile)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("publish", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def repoConfigFile = new File(testProjectDir, "build/pubrepo/com/intershop/test/rootproject/10.0.0/rootproject-10.0.0-configuration.zip")

        then:
        result.task(':publish').outcome == SUCCESS
        result.task(':zipConfiguration').outcome == SUCCESS
        repoConfigFile.exists()

        where:
        gradleVersion << supportedGradleVersions
    }
}
