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

import com.intershop.gradle.icm.tasks.CreateServerInfo
import com.intershop.gradle.test.AbstractIntegrationKotlinSpec

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE 

class ICMBasePluginIntegrationKotlinSpec extends AbstractIntegrationKotlinSpec {

    def 'Run complete configuration'() {
        given:
        settingsFile << """
        rootProject.name = "rootproject"
        """.stripIndent()

        buildFile << """
            plugins {
                id("com.intershop.gradle.icm.base")
            }
            
            group = "com.intershop.test"
            version = "7.11.0.0"
            
            intershop {
            
                projectInfo {
                    productID.set("ICM 7 B2C")
                    productName.set("Intershop Commerce Management 7 B2C")
                    copyrightOwner.set("Intershop Communications")
                    copyrightFrom.set("2005")
                    organization.set("Intershop Communications")
                }
            
                mavenPublicationName.set("ishmvn")
            }
            
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
        
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = uri("\${project.rootProject.buildDir}/pubrepo")
                }
            }
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
        
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = uri("\${project.rootProject.buildDir}/pubrepo")
                }
            }
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
        
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = uri("\${project.rootProject.buildDir}/pubrepo")
                }
            }
        }   
        """.stripIndent())

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("createServerInfo")
                .withGradleVersion(gradleVersion)
                .build()

        File f = new File(testProjectDir, "build/${CreateServerInfo.VERSIONINFO}")
        Properties props = new Properties()
        props.load(f.newDataInputStream())

        then:
        result.task(':createServerInfo').outcome == SUCCESS
        props.getProperty("version.information.productName") == "Intershop Commerce Management 7 B2C"

        when:
        def resultpub = getPreparedGradleRunner()
                .withArguments("publish")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultpub.task(':publish').outcome == UP_TO_DATE
        resultpub.task(':prjCartridge_adapter:zipStaticFiles').outcome == SUCCESS
        resultpub.task(':prjCartridge_adapter:writeCartridgeDescriptor').outcome == SUCCESS
        resultpub.task(':prjCartridge_adapter:generateMetadataFileForIshmvnPublication').outcome == SUCCESS
        resultpub.task(':prjCartridge_adapter:generatePomFileForIshmvnPublication').outcome == SUCCESS
        resultpub.task(':prjCartridge_adapter:publishIshmvnPublicationToMavenRepository').outcome == SUCCESS
        resultpub.task(':prjCartridge_adapter:publish').outcome == SUCCESS

        resultpub.task(':prjCartridge_dev:zipStaticFiles').outcome == SUCCESS
        resultpub.task(':prjCartridge_dev:writeCartridgeDescriptor').outcome == SUCCESS
        resultpub.task(':prjCartridge_dev:generateMetadataFileForIshmvnPublication').outcome == SUCCESS
        resultpub.task(':prjCartridge_dev:generatePomFileForIshmvnPublication').outcome == SUCCESS
        resultpub.task(':prjCartridge_dev:publishIshmvnPublicationToMavenRepository').outcome == SUCCESS
        resultpub.task(':prjCartridge_dev:publish').outcome == SUCCESS

        resultpub.task(':prjCartridge_prod:zipStaticFiles') == null
        resultpub.task(':prjCartridge_prod:writeCartridgeDescriptor').outcome == SUCCESS
        resultpub.task(':prjCartridge_prod:generateMetadataFileForIshmvnPublication').outcome == SUCCESS
        resultpub.task(':prjCartridge_prod:generatePomFileForIshmvnPublication').outcome == SUCCESS
        resultpub.task(':prjCartridge_prod:publishIshmvnPublicationToMavenRepository').outcome == SUCCESS
        resultpub.task(':prjCartridge_prod:publish').outcome == SUCCESS

        resultpub.task(':prjCartridge_test:publish') == null
        where:
        gradleVersion << supportedGradleVersions
    }
}
