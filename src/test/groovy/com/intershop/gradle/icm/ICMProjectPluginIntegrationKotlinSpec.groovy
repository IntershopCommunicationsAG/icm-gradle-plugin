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

    def 'check product cartridge list and setup cartridges'() {
        prepareDefaultBuildConfig(testProjectDir, settingsFile, buildFile)

        when:
        def resultSC = getPreparedGradleRunner()
                .withArguments("setupCartridgesProd", "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridges = new File(testProjectDir, "build/container/cartridges")
        def adapter_cartridge = new File(testProjectDir, "build/container/cartridges/cartridge_adapter/release/lib/cartridge_adapter-1.0.0.jar")
        def prod_cartridge = new File(testProjectDir, "build/container/cartridges/cartridge_prod/release/lib/cartridge_prod-1.0.0.jar")
        def cartridges_libs_library2 = new File(testProjectDir, "build/container/cartridges/libs/com.other-library2-1.5.0.jar")
        def cartridges_libs_library3 = new File(testProjectDir, "build/container/cartridges/libs/com.other-library3-1.5.0.jar")
        def templateFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        resultSC.task(":setupCartridgesProd").outcome == SUCCESS
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
        def resultSC = getPreparedGradleRunner()
                .withArguments("setupCartridgesTest",  "-s", "--warning-mode", "all")
                .withGradleVersion(gradleVersion)
                .build()

        def cartridges = new File(testProjectDir, "build/testcontainer/cartridges")
        def test_cartridge = new File(testProjectDir, "build/testcontainer/cartridges/cartridge_test/release/lib/cartridge_test-1.0.0.jar")
        def cartridges_libs_library3 = new File(testProjectDir, "build/testcontainer/cartridges/libs/com.other-library3-1.5.0.jar")
        def templateFile = new File(testProjectDir, "build/libfilter/libfilter.txt")

        then:
        resultSC.task(":setupCartridgesTest").outcome == SUCCESS
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
        def resultSC = getPreparedGradleRunner()
                .withArguments("setupCartridges", "-s", "--warning-mode", "all")
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
        resultSC.task(":setupCartridges").outcome == SUCCESS
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
                        platform("com.intershop:libbom:1.0.0")
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
        def productionLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

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
        configClusterDir.listFiles().size() == 2
        productionLibsDir.exists()
        productionLibsDir.listFiles().size() == 13
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
        def cartridgesLibDir = new File(cartridgesDir, "libs")
        def configDir = new File(testProjectDir, "build/server/config_folder/system-conf" )
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def productionLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

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

        def cartridgesDir = new File(testProjectDir, "build/container/cartridges")
        def cartridgesLibDir = new File(cartridgesDir, "libs")
        def configDir = new File(testProjectDir, "build/container/config_folder/system-conf" )
        def configAppsDir = new File(configDir, "apps")
        def configClusterDir = new File(configDir, "cluster")
        def productionLibsDir = new File(testProjectDir, "build/libraries/production")
        def testLibsDir = new File(testProjectDir, "build/libraries/test")

        then:
        result.task(':prepareContainer').outcome == SUCCESS
        result.output.contains("No library filter is available!")
        cartridgesDir.exists()
        cartridgesDir.listFiles().size() == 3
        cartridgesLibDir.exists()
        cartridgesLibDir.listFiles().size() == 2
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
