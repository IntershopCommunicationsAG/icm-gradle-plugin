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

import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS 

class ICMProjectPluginIntegrationKotlinSpec extends AbstractIntegrationKotlinSpec {


    final String PROD_CARTRIDGES = "runtime pf_cartridge component file emf pf_extension pf_property jmx app pf_kafka cache pipeline isml orm configuration businessobject core orm_oracle orm_mssql wsrp rest bc_auditing bc_region bc_service bc_mail bc_ruleengine report bc_auditing_orm bc_organization bc_approval bc_validation bc_address bc_address_orm bc_user bc_user_orm bc_captcha bc_pdf bc_abtest bc_abtest_orm bc_payment sld_pdf sld_preview sld_mcm sld_ch_b2c_base sld_ch_sf_base app_bo_catalog ac_gtm dev_basketinfo dev_apiinfo prjCartridge_prod cartridge_adapter prjCartridge_adapter cartridge_prod"
    final String PROD_DB_CARTRIDGES = "runtime file emf pf_extension pf_property jmx app pf_kafka cache pipeline isml orm orm_oracle orm_mssql wsrp rest bc_user_orm bc_captcha btc monitor smc bc_pricing bc_pmc pmc_rest bc_search bc_mvc bc_order bc_order_orm sld_mcm sld_ch_b2c_base sld_ch_sf_base ac_bmecat ac_oci ac_cxml prjCartridge_prod"

    final String TEST_CARTRIDGES = "runtime pf_cartridge component file emf pf_extension pf_property jmx app pf_kafka cache pipeline isml orm configuration businessobject core orm_oracle orm_mssql wsrp rest bc_auditing bc_region bc_service bc_mail bc_ruleengine report bc_auditing_orm bc_organization bc_approval bc_validation bc_address bc_address_orm bc_user bc_user_orm bc_captcha bc_pdf bc_abtest bc_abtest_orm bc_payment sld_pdf sld_preview sld_mcm sld_ch_b2c_base sld_ch_sf_base app_bo_catalog ac_gtm dev_basketinfo dev_apiinfo cartridge_test prjCartridge_prod cartridge_adapter prjCartridge_adapter prjCartridge_test cartridge_prod"
    final String TEST_DB_CARTRIDGES = "runtime file emf pf_extension pf_property jmx app pf_kafka cache pipeline isml orm orm_oracle orm_mssql wsrp rest bc_user_orm bc_captcha btc monitor smc bc_pricing bc_pmc pmc_rest bc_search bc_mvc bc_order bc_order_orm sld_mcm sld_ch_b2c_base sld_ch_sf_base ac_bmecat ac_oci ac_cxml prjCartridge_prod prjCartridge_test"

    final String CARTRIDGES = "runtime pf_cartridge component file emf pf_extension pf_property jmx app pf_kafka cache pipeline isml orm configuration businessobject core orm_oracle orm_mssql wsrp rest bc_auditing bc_region bc_service bc_mail bc_ruleengine report bc_auditing_orm bc_organization bc_approval bc_validation bc_address bc_address_orm bc_user bc_user_orm bc_captcha bc_pdf bc_abtest bc_abtest_orm bc_payment sld_pdf sld_preview sld_mcm sld_ch_b2c_base sld_ch_sf_base app_bo_catalog ac_gtm dev_basketinfo dev_apiinfo cartridge_test prjCartridge_prod cartridge_dev cartridge_adapter prjCartridge_adapter prjCartridge_dev prjCartridge_test cartridge_prod"
    final String DB_CARTRIDGES = "runtime file emf pf_extension pf_property jmx app pf_kafka cache pipeline isml orm orm_oracle orm_mssql wsrp rest bc_user_orm bc_captcha btc monitor smc bc_pricing bc_pmc pmc_rest bc_search bc_mvc bc_order bc_order_orm sld_mcm sld_ch_b2c_base sld_ch_sf_base ac_bmecat ac_oci ac_cxml prjCartridge_prod prjCartridge_test"

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
        result.task(':showConfPath').outcome == SUCCESS
        result.output.contains(".gradle/icm-default/conf/cluster.properties")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-DconfigDir=/home/user/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showConfPath').outcome == SUCCESS
        result1.output.contains("/home/user/conf/cluster.properties")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-PconfigDir=/home/otheruser/conf")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showConfPath').outcome == SUCCESS
        result2.output.contains("/home/otheruser/conf/cluster.properties")

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all")
                .withEnvironment([ "CONFIGDIR": "/home/other/conf" ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showConfPath').outcome == SUCCESS
        result3.output.contains("/home/other/conf/cluster.properties")

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'check product cartridge list and setup cartridges'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultECL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_PROD, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()
        def cartridgeListProd = new File(testProjectDir, "build/container/cartridgelist/cartridgelist.properties")

        then:
        resultECL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_PROD}").outcome == SUCCESS
        cartridgeListProd.exists()

        when:
        Properties properties = new Properties()
        cartridgeListProd.withInputStream {
            properties.load(it)
        }

        then:
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_PROPERTY) == PROD_CARTRIDGES
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_DBINIT_PROPERTY) == PROD_DB_CARTRIDGES

        when:
        def resultSC = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_PROD, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridges = new File(testProjectDir, "build/container/cartridges")
        def adapter_cartridge = new File(testProjectDir, "build/container/cartridges/cartridge_adapter/release/lib/cartridge_adapter-1.0.0.jar")
        def prod_cartridge = new File(testProjectDir, "build/container/cartridges/cartridge_prod/release/lib/cartridge_prod-1.0.0.jar")
        def cartridges_libs_library2 = new File(testProjectDir, "build/container/cartridges/libs/com.other-library2-1.5.0.jar")
        def cartridges_libs_library3 = new File(testProjectDir, "build/container/cartridges/libs/com.other-library3-1.5.0.jar")
        def templateFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        resultSC.task(":${com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_PROD}").outcome == SUCCESS
        cartridges.exists()
        adapter_cartridge.exists()
        prod_cartridge.exists()
        ! cartridges_libs_library2.exists()
        cartridges_libs_library3.exists()
        templateFile.exists()

        def libs = []
        templateFile.eachLine { line ->
            libs << line
        }
        libs.contains("com.other-library2-1.5.0")
        cartridges.listFiles().size() == 3

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'check test cartridge list and setup cartridges'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultECL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_TEST, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()
        def cartridgeListTest = new File(testProjectDir, "build/testcontainer/cartridgelist/cartridgelist.properties")

        then:
        resultECL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST_TEST}").outcome == SUCCESS
        cartridgeListTest.exists()

        when:
        Properties properties = new Properties()
        cartridgeListTest.withInputStream {
            properties.load(it)
        }

        then:
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_PROPERTY) == TEST_CARTRIDGES
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_DBINIT_PROPERTY) == TEST_DB_CARTRIDGES

        when:
        def resultSC = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_TEST,  "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridges = new File(testProjectDir, "build/testcontainer/cartridges")
        def test_cartridge = new File(testProjectDir, "build/testcontainer/cartridges/cartridge_test/release/lib/cartridge_test-1.0.0.jar")
        def cartridges_libs_library3 = new File(testProjectDir, "build/testcontainer/cartridges/libs/com.other-library3-1.5.0.jar")
        def templateFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        resultSC.task(":${com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES_TEST}").outcome == SUCCESS
        cartridges.exists()
        test_cartridge.exists()
        cartridges_libs_library3.exists()
        templateFile.exists()
        cartridges.listFiles().size() == 2

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'check dev cartridge list and setup cartridges'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultECL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()
        def cartridgeList = new File(testProjectDir, "build/server/cartridgelist/cartridgelist.properties")

        then:
        resultECL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.EXTEND_CARTRIDGELIST}").outcome == SUCCESS
        cartridgeList.exists()

        when:
        Properties properties = new Properties()
        cartridgeList.withInputStream {
            properties.load(it)
        }

        then:
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_PROPERTY) == CARTRIDGES
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_DBINIT_PROPERTY) == DB_CARTRIDGES

        when:
        def resultSC = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridges = new File(testProjectDir, "build/server/cartridges")
        def adapter_cartridge = new File(testProjectDir, "build/server/cartridges/cartridge_adapter/release/lib/cartridge_adapter-1.0.0.jar")
        def prod_cartridge = new File(testProjectDir, "build/server/cartridges/cartridge_prod/release/lib/cartridge_prod-1.0.0.jar")
        def dev_cartridge = new File(testProjectDir, "build/server/cartridges/cartridge_dev/release/lib/cartridge_dev-1.0.0.jar")
        def test_cartridge = new File(testProjectDir, "build/server/cartridges/cartridge_test/release/lib/cartridge_test-1.0.0.jar")
        def cartridges_libs_library3 = new File(testProjectDir, "build/server/cartridges/libs/com.other-library3-1.5.0.jar")
        def templateFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        resultSC.task(":${com.intershop.gradle.icm.ICMProjectPlugin.SETUP_CARTRIDGES}").outcome == SUCCESS
        cartridges.exists()
        adapter_cartridge.exists()
        prod_cartridge.exists()
        dev_cartridge.exists()
        test_cartridge.exists()
        cartridges_libs_library3.exists()
        templateFile.exists()
        cartridges.listFiles().size() == 5

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'copy 3rd party libs'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def result3RDPL = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_PROD, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def prodDir = new File(testProjectDir, "build/container/prjlibs")

        then:
        result3RDPL.task(":${com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_PROD}").outcome == SUCCESS
        prodDir.exists()
        new File(prodDir, "com.google.guava-guava-16.0.1.jar").exists()
        ! new File(prodDir, "org.codehaus.janino-janino-2.5.16.jar").exists()
        new File(prodDir, "org.slf4j-slf4j-api-1.7.25.jar").exists()
        new File(testProjectDir, "build/libfilter/libfilter.txt").exists()

        when:
        def result3RDPLTest = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_TEST, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def testDir = new File(testProjectDir, "build/testcontainer/prjlibs")

        then:
        result3RDPLTest.task(":${com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS_TEST}").outcome == SUCCESS
        testDir.exists()
        ! new File(testDir, "com.google.guava-guava-16.0.1.jar").exists()
        new File(testDir, "org.codehaus.janino-janino-2.5.16.jar").exists()
        new File(testDir, "org.slf4j-slf4j-api-1.7.25.jar").exists()
        new File(testProjectDir, "build/libfilter/libfilter.txt").exists()

        when:
        def result3RDPLDev = getPreparedGradleRunner()
                .withArguments(com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS, "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def devDir = new File(testProjectDir, "build/server/prjlibs")

        then:
        result3RDPLDev.task(":${com.intershop.gradle.icm.ICMProjectPlugin.COPY_LIBS}").outcome == SUCCESS
        devDir.exists()
        new File(devDir, "com.google.guava-guava-16.0.1.jar").exists()
        new File(devDir, "org.codehaus.janino-janino-2.5.16.jar").exists()
        new File(devDir, "org.slf4j-slf4j-api-1.7.25.jar").exists()
        new File(testProjectDir, "build/libfilter/libfilter.txt").exists()

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
        createLocalFile("config/base/cluster/cartridgelist.properties", "cartridgelist = base_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties", "dev_test = 1")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")

        createLocalFile("sites/base/test-site1/units/test.properties", "test-site1-sites = base")
        createLocalFile("sites/base/test-site2/units/test.properties", "test-site2-sites = 2")
        createLocalFile("sites/prod/test-site1/units/test.properties", "test-site1-sites = prod")
        createLocalFile("sites/test/test-site1/units/test.properties", "test-site1-sites = test")
        createLocalFile("sites/dev/test-site1/units/test.properties", "test-site1-sites = dev")

        buildFile << """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
            }

            group = "com.intershop.test"
            version = "10.0.0"

            intershop {
                projectConfig {
                    
                    cartridges.set(listOf( "com.intershop.cartridge:cartridge_test:1.0.0", 
                                   "prjCartridge_prod",
                                   "com.intershop.cartridge:cartridge_dev:1.0.0", 
                                   "com.intershop.cartridge:cartridge_adapter:1.0.0",
                                   "prjCartridge_adapter",
                                   "prjCartridge_dev",
                                   "prjCartridge_test",
                                   "com.intershop.cartridge:cartridge_prod:1.0.0" ))

                    dbprepareCartridges.set(listOf( "prjCartridge_prod",
                                            "prjCartridge_test" ))

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                        }
                    }

                    serverDirConfig {
                        base {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/base"))
                                        exclude("**/test-site1/units/test.properties")
                                    }
                                }
                            }
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/base"))
                                        exclude("**/cluster/test.properties")
                                    }
                                }
                                exclude("**/cluster/cartridgelist.properties")
                            }
                        }
                        prod {
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/prod"))
                                    }
                                }
                            }
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/prod"))
                                    }
                                }
                            }
                        }
                        test {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/test"))
                                    }
                                }
                            }
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/test"))
                                    }
                                }
                            }
                        }
                        dev {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/dev"))
                                    }
                                    register("test") {
                                        dir.set(file("sites/test"))
                                        exclude("**/units/test.properties")
                                    }
                                }
                            }
                            config {
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
            }

            ${repoConf}
        """.stripIndent()

        def prj1dir = createSubProject('prjCartridge_prod', """
        plugins {
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
            jcenter()
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
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
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_PROD}", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridgeListProd = new File(testProjectDir, "build/container/config_folder/system-conf/cluster/cartridgelist.properties")

        Properties properties = new Properties()
        cartridgeListProd.withInputStream {
            properties.load(it)
        }

        def versionFile = new File(testProjectDir,"build/container/config_folder/system-conf/cluster/version.properties")
        def apps1File = new File(testProjectDir,"build/container/config_folder/system-conf/apps/file.txt")
        def apps2File = new File(testProjectDir,"build/container/config_folder/system-conf/apps/file2.txt")
        def apps3File = new File(testProjectDir,"build/container/config_folder/system-conf/apps/paymenr.txt")
        def testPropsFile = new File(testProjectDir,"build/container/config_folder/system-conf/cluster/test.properties")

        then:
        resultProdConf.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_PROD}").outcome == SUCCESS

        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_PROPERTY) == PROD_CARTRIDGES
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_DBINIT_PROPERTY) == PROD_DB_CARTRIDGES

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
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_TEST}", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridgeListProd = new File(testProjectDir, "build/testcontainer/config_folder/system-conf/cluster/cartridgelist.properties")

        Properties properties = new Properties()
        cartridgeListProd.withInputStream {
            properties.load(it)
        }

        def versionFile = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/cluster/version.properties")
        def apps1File = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/apps/file.txt")
        def apps2File = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/apps/file2.txt")
        def apps3File = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/apps/paymenr.txt")
        def testPropsFile = new File(testProjectDir,"build/testcontainer/config_folder/system-conf/cluster/test.properties")

        then:
        resultProdConf.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER_TEST}").outcome == SUCCESS

        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_PROPERTY) == TEST_CARTRIDGES
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_DBINIT_PROPERTY) == TEST_DB_CARTRIDGES

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
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER}", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridgeListProd = new File(testProjectDir, "build/server/config_folder/system-conf/cluster/cartridgelist.properties")

        Properties properties = new Properties()
        cartridgeListProd.withInputStream {
            properties.load(it)
        }

        def versionFile = new File(testProjectDir,"build/server/config_folder/system-conf/cluster/version.properties")
        def apps1File = new File(testProjectDir,"build/server/config_folder/system-conf/apps/file.txt")
        def apps2File = new File(testProjectDir,"build/server/config_folder/system-conf/apps/file2.txt")
        def apps3File = new File(testProjectDir,"build/server/config_folder/system-conf/apps/paymenr.txt")
        def testPropsFile = new File(testProjectDir,"build/server/config_folder/system-conf/cluster/test.properties")

        then:
        resultProdConf.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_CONFIGFOLDER}").outcome == SUCCESS

        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_PROPERTY) == CARTRIDGES
        properties.getProperty(com.intershop.gradle.icm.tasks.ExtendCartridgeList.CARTRIDGES_DBINIT_PROPERTY) == DB_CARTRIDGES

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

    def "test of prod tasks for sites folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultDevSites = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_PROD}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def sitesFolder = new File(testProjectDir,"build/container/sites_folder/sites")

        then:
        resultDevSites.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_PROD}").outcome == SUCCESS
        sitesFolder.exists()
        sitesFolder.listFiles().size() == 8
        new File(sitesFolder, "test-site1/units/test.properties").text.contains("test-site1-sites = prod")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test of test tasks for sites folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultDevSites = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_TEST}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def sitesFolder = new File(testProjectDir,"build/testcontainer/sites_folder/sites")

        then:
        resultDevSites.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER_TEST}").outcome == SUCCESS
        sitesFolder.exists()
        sitesFolder.listFiles().size() == 8
        new File(sitesFolder, "test-site1/units/test.properties").text.contains("test-site1-sites = test")

        where:
        gradleVersion << supportedGradleVersions
    }

    def "test of development tasks for sites folder creation"() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultDevSites = getPreparedGradleRunner()
                .withArguments("${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER}", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        def sitesFolder = new File(testProjectDir,"build/server/sites_folder/sites")

        then:
        resultDevSites.task(":${com.intershop.gradle.icm.ICMProjectPlugin.CREATE_SITESFOLDER}").outcome == SUCCESS
        sitesFolder.exists()
        sitesFolder.listFiles().size() == 8
        new File(sitesFolder, "test-site1/units/test.properties").text.contains("test-site1-sites = dev")

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
                    
                    cartridges.set(listOf("com.intershop.cartridge:cartridge_test:1.0.0", 
                                   "prjCartridge_prod",
                                   "com.intershop.cartridge:cartridge_dev:1.0.0", 
                                   "com.intershop.cartridge:cartridge_adapter:1.0.0",
                                   "prjCartridge_adapter",
                                   "prjCartridge_dev",
                                   "prjCartridge_test",
                                   "com.intershop.cartridge:cartridge_prod:1.0.0"))

                    dbprepareCartridges.set(listOf("prjCartridge_prod",
                                            "prjCartridge_test"))

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                    }

                    cartridgeListDependency.set("com.project.group:configuration:1.0.0")

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
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments(":provideCartridgeListTemplate", "-s")
                .withGradleVersion(gradleVersion)
                .build()
        def clFile = new File(testProjectDir, "build/cartridgelisttemplate/cartridgelist.properties")

        then:
        result.task(":provideCartridgeListTemplate").outcome == SUCCESS
        clFile.exists()
        clFile.text.contains("simple file on repo")

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

        def cartridgesDir = new File(testProjectDir, "build/container/cartridges")
        def cartridgesLibDir = new File(cartridgesDir, "libs")
        def configDir = new File(testProjectDir, "build/container/config_folder/system-conf" )
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def prjLibsDir = new File(testProjectDir, "build/container/prjlibs")
        def sitesDir = new File(testProjectDir, "build/container/sites_folder/sites")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        cartridgesDir.exists()
        cartridgesDir.listFiles().size() == 3
        cartridgesLibDir.exists()
        cartridgesLibDir.listFiles().size() == 1
        configDir.exists()
        configDir.listFiles().size() == 2
        configAppsDir.exists()
        configAppsDir.listFiles().size() == 3
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 3
        prjLibsDir.exists()
        prjLibsDir.listFiles().size() == 2
        sitesDir.exists()
        sitesDir.listFiles().size() == 8

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
        def cartridgesLibDir = new File(cartridgesDir, "libs")
        def configDir = new File(testProjectDir, "build/server/config_folder/system-conf" )
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def prjLibsDir = new File(testProjectDir, "build/server/prjlibs")
        def sitesDir = new File(testProjectDir, "build/server/sites_folder/sites")

        then:
        result.task(':prepareServer').outcome == SUCCESS
        cartridgesDir.exists()
        cartridgesDir.listFiles().size() == 5
        cartridgesLibDir.exists()
        cartridgesLibDir.listFiles().size() == 1
        configDir.exists()
        configDir.listFiles().size() == 2
        configAppsDir.exists()
        configAppsDir.listFiles().size() == 3
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 3
        prjLibsDir.exists()
        prjLibsDir.listFiles().size() == 3
        sitesDir.exists()
        sitesDir.listFiles().size() == 8

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

        createLocalFile("sites/base/test-site1/units/test.properties", "test-site1-sites = base")
        createLocalFile("sites/base/test-site2/units/test.properties", "test-site2-sites = 2")
        createLocalFile("sites/base/test-site3/units/test.properties", "test-site3-sites = 3")
        createLocalFile("sites/prod/test-site1/units/test.properties", "test-site1-sites = prod")
        createLocalFile("sites/test/test-site1/units/test.properties", "test-site1-sites = test")
        createLocalFile("sites/dev/test-site1/units/test.properties", "test-site1-sites = dev")

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
                  
                    cartridges.set(listOf("com.intershop.cartridge:cartridge_test:1.0.0", 
                                   "prjCartridge_prod",
                                   "com.intershop.cartridge:cartridge_dev:1.0.0", 
                                   "com.intershop.cartridge:cartridge_adapter:1.0.0",
                                   "prjCartridge_adapter",
                                   "prjCartridge_dev",
                                   "prjCartridge_test",
                                   "com.intershop.cartridge:cartridge_prod:1.0.0"))

                    dbprepareCartridges.set(listOf("prjCartridge_prod",
                                            "prjCartridge_test"))

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                        }
                    }

                    serverDirConfig {
                        base {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/base"))
                                        exclude("**/test-site1/units/test.properties")
                                    }
                                }
                            }
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/base"))
                                        exclude("**/cluster/test.properties")
                                    }
                                }
                                exclude("**/cluster/cartridgelist.properties")
                            }
                        }
                        prod {
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/prod"))
                                    }
                                }
                            }
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/prod"))
                                    }
                                }
                            }
                        }
                        test {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/test"))
                                    }
                                }
                            }
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/test"))
                                    }
                                }
                            }
                        }
                        dev {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/dev"))
                                    }
                                    register("test") {
                                        dir.set(file("sites/test"))
                                        exclude("**/units/test.properties")
                                    }
                                }
                            }
                            config {
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
        def repoSitesFile = new File(testProjectDir, "build/pubrepo/com/intershop/test/rootproject/10.0.0/rootproject-10.0.0-sites.zip")

        then:
        result.task(':publish').outcome == SUCCESS
        result.task(':zipSites').outcome == SUCCESS
        result.task(':zipConfiguration').outcome == SUCCESS
        repoConfigFile.exists()
        repoSitesFile.exists()
        new ZipFile(repoConfigFile).entries().toList().size() == 3
        new ZipFile(repoSitesFile).entries().toList().size() == 10

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

        createLocalFile("sites/base/test-site1/units/test.properties", "test-site1-sites = base")
        createLocalFile("sites/base/test-site2/units/test.properties", "test-site2-sites = 2")
        createLocalFile("sites/base/test-site3/units/test.properties", "test-site3-sites = 3")
        createLocalFile("sites/prod/test-site1/units/test.properties", "test-site1-sites = prod")
        createLocalFile("sites/test/test-site1/units/test.properties", "test-site1-sites = test")
        createLocalFile("sites/dev/test-site1/units/test.properties", "test-site1-sites = dev")

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
                    
                    cartridges.set(listOf("com.intershop.cartridge:cartridge_test:1.0.0", 
                                   "prjCartridge_prod",
                                   "com.intershop.cartridge:cartridge_dev:1.0.0", 
                                   "com.intershop.cartridge:cartridge_adapter:1.0.0",
                                   "prjCartridge_adapter",
                                   "prjCartridge_dev",
                                   "prjCartridge_test",
                                   "com.intershop.cartridge:cartridge_prod:1.0.0"))

                    dbprepareCartridges.set(listOf("prjCartridge_prod",
                                            "prjCartridge_test"))

                    base {
                        dependency.set("com.intershop.icm:icm-as:2.0.0")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                        }
                    }

                    serverDirConfig {
                        base {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/base"))
                                        exclude("**/test-site1/units/test.properties")
                                    }
                                }
                            }
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/base"))
                                        exclude("**/cluster/test.properties")
                                    }
                                }
                                exclude("**/cluster/cartridgelist.properties")
                            }
                        }
                        prod {
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/prod"))
                                    }
                                }
                            }
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/prod"))
                                    }
                                }
                            }
                        }
                        test {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/test"))
                                    }
                                }
                            }
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/test"))
                                    }
                                }
                            }
                        }
                        dev {
                            sites {
                                dirs {
                                    register("main") {
                                        dir.set(file("sites/dev"))
                                    }
                                    register("test") {
                                        dir.set(file("sites/test"))
                                        exclude("**/units/test.properties")
                                    }
                                }
                            }
                            config {
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

        def cartridgesDir = new File(testProjectDir, "build/container/cartridges")
        def cartridgesLibDir = new File(cartridgesDir, "libs")
        def configDir = new File(testProjectDir, "build/container/config_folder/system-conf" )
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def prjLibsDir = new File(testProjectDir, "build/container/prjlibs")
        def sitesDir = new File(testProjectDir, "build/container/sites_folder/sites")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        result.output.contains("No library filter is available!")
        cartridgesDir.exists()
        cartridgesDir.listFiles().size() == 3
        cartridgesLibDir.exists()
        cartridgesLibDir.listFiles().size() == 1
        configDir.exists()
        configDir.listFiles().size() == 2
        configAppsDir.exists()
        configAppsDir.listFiles().size() == 3
        configClusterDir.exists()
        configClusterDir.listFiles().size() == 3
        prjLibsDir.exists()
        prjLibsDir.listFiles().size() == 13
        sitesDir.exists()
        sitesDir.listFiles().size() == 9

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
                    
                    cartridges.set(listOf("com.intershop.cartridge:cartridge_test:1.0.0", 
                                   "prjCartridge_prod",
                                   "com.intershop.cartridge:cartridge_dev:1.0.0", 
                                   "com.intershop.cartridge:cartridge_adapter:1.0.0",
                                   "prjCartridge_adapter",
                                   "prjCartridge_dev",
                                   "prjCartridge_test",
                                   "com.intershop.cartridge:cartridge_prod:1.0.0"))

                    dbprepareCartridges.set(listOf("prjCartridge_prod",
                                            "prjCartridge_test"))

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
                    }

                    modules {
                        register("solrExt") {
                            dependency.set("com.intershop.search:solrcloud:1.0.0")
                        }
                        register("paymentExt") {
                            dependency.set("com.intershop.payment:paymenttest:1.0.0")
                        }
                    }

                    serverDirConfig {
                        base {
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/base"))
                                        exclude("**/cluster/test.properties")
                                    }
                                }
                                exclude("**/cluster/cartridgelist.properties")
                            }
                        }
                        prod {
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/prod"))
                                    }
                                }
                            }
                        }
                        test {
                            config {
                                dirs {
                                    register("main") {
                                        dir.set(file("config/test"))
                                    }
                                }
                            }
                        }
                        dev {
                            config {
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
        def repoSitesFile = new File(testProjectDir, "build/pubrepo/com/intershop/test/rootproject/10.0.0/rootproject-10.0.0-sites.zip")

        then:
        result.task(':publish').outcome == SUCCESS
        result.task(':zipSites').outcome == NO_SOURCE
        result.task(':zipConfiguration').outcome == SUCCESS
        repoConfigFile.exists()
        ! repoSitesFile.exists()
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
                    
                    cartridges.set(listOf("com.intershop.cartridge:cartridge_test:1.0.0", 
                                   "prjCartridge_prod",
                                   "com.intershop.cartridge:cartridge_dev:1.0.0", 
                                   "com.intershop.cartridge:cartridge_adapter:1.0.0",
                                   "prjCartridge_adapter",
                                   "prjCartridge_dev",
                                   "prjCartridge_test",
                                   "com.intershop.cartridge:cartridge_prod:1.0.0"))

                    dbprepareCartridges.set(listOf("prjCartridge_prod",
                                            "prjCartridge_test"))

                    base {
                        dependency.set("com.intershop.icm:icm-as:1.0.0")
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
        def repoSitesFile = new File(testProjectDir, "build/pubrepo/com/intershop/test/rootproject/10.0.0/rootproject-10.0.0-sites.zip")

        then:
        result.task(':publish').outcome == SUCCESS
        result.task(':zipSites').outcome == NO_SOURCE
        result.task(':zipConfiguration').outcome == NO_SOURCE
        ! repoConfigFile.exists()
        ! repoSitesFile.exists()

        where:
        gradleVersion << supportedGradleVersions
    }
}
