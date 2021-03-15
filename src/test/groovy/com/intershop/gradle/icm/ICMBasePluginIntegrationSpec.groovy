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

import com.intershop.gradle.icm.tasks.CreateInitPackage
import com.intershop.gradle.icm.tasks.CreateInitTestPackage
import com.intershop.gradle.icm.tasks.CreateMainPackage
import com.intershop.gradle.icm.tasks.CreateTestPackage
import com.intershop.gradle.icm.tasks.CreateServerInfo

import com.intershop.gradle.test.AbstractIntegrationGroovySpec
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Ignore

import static org.gradle.testkit.runner.TaskOutcome.*

class ICMBasePluginIntegrationSpec extends AbstractIntegrationGroovySpec {

    def 'Plugin is applied to the root project'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.base'
            }
        """.stripIndent()

        File prjDir1 = createSubProject(":subprj1",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        writeJavaTestClass("com.intershop.test.Test1", prjDir1)

        File prjDir2 = createSubProject(":subprj2",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        writeJavaTestClass("com.intershop.test.Test2", prjDir2)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("projects", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':projects').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Plugin can not be applied to the sub project'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        createSubProject(":subprj1",
                """
                plugins {
                    id 'java-library'
                    id 'com.intershop.gradle.icm.base'
                }
                """.stripIndent())

        createSubProject(":subprj2",
                """
                plugins {
                    id 'java-library'
                }
                """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("projects")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':projects').outcome == SUCCESS
        result.output.contains("ICM build plugin will be not applied to the sub project 'subprj1'")

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'CreateServerInfo is configured and works with default'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.base'
            }
            
            version = '1.0.0'

            intershop {
            }
            
        """.stripIndent()

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
        props.getProperty("version.information.productName") == "Intershop Commerce Management 7"

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'CreateServerInfo is configured and works with changed configuration'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.base'
            }
            
            version = '1.0.0'

            intershop {
                projectInfo {
                    productName = "Intershop Commerce Management 7 B2C"        
                }       
            }
        """.stripIndent()

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

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'CopyThirdpartyLibs of subprojects to empty folder'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            repositories {
                mavenCentral()
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
            
            implementation "junit:junit:4.13.1"
            implementation "org.junit.jupiter:junit-jupiter:5.7.0"
            implementation "org.junit.jupiter:junit-jupiter-api:5.7.0"
            implementation "org.junit.jupiter:junit-jupiter-params:5.7.0"
            implementation "org.junit.platform:junit-platform-commons:1.7.0"
            implementation "org.junit.platform:junit-platform-runner:1.7.0"
            implementation "org.junit.vintage:junit-vintage-engine:5.7.0"
        } 
        
        repositories {
            mavenCentral()
        }
        """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge'
        }
        
        dependencies {
            implementation project(":testCartridge1")
            implementation 'org.codehaus.janino:janino:2.5.16'
            implementation 'org.codehaus.janino:commons-compiler:3.0.6'
            implementation 'ch.qos.logback:logback-core:1.2.3'
            implementation 'ch.qos.logback:logback-classic:1.2.3'
            implementation 'commons-collections:commons-collections:3.2.2'
        } 
        
        repositories {
            mavenCentral()
        }        
        """.stripIndent())

        writeJavaTestClass("com.intershop.testCartridge1", prj1dir)
        writeJavaTestClass("com.intershop.testCartridge2", prj2dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("copyThirdpartyLibs", '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:copyThirdpartyLibs').outcome == SUCCESS
        result.task(':testCartridge2:copyThirdpartyLibs').outcome == SUCCESS

        (new File(testProjectDir,"testCartridge2/build/lib/ch.qos.logback-logback-classic-1.2.3.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/com.google.inject-guice-4.0.jar")).exists()

        (new File(testProjectDir,"testCartridge1/build/lib/junit-junit-4.13.1.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/org.junit.jupiter-junit-jupiter-5.7.0.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/org.junit.jupiter-junit-jupiter-api-5.7.0.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/org.junit.jupiter-junit-jupiter-params-5.7.0.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/org.junit.platform-junit-platform-commons-1.7.0.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/org.junit.platform-junit-platform-runner-1.7.0.jar")).exists()
        (new File(testProjectDir,"testCartridge1/build/lib/org.junit.vintage-junit-vintage-engine-5.7.0.jar")).exists()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("copyThirdpartyLibs", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':testCartridge1:copyThirdpartyLibs').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':testCartridge2:copyThirdpartyLibs').outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'CopyThirdpartyLibs of subprojects with changed dependencies'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            version = "1.0.0"
            
            repositories {
                mavenCentral()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge'
        }
        
        dependencies {
            implementation "com.google.inject:guice:\${project.ext.inject_guice_version}"
            implementation 'com.google.inject.extensions:guice-servlet:3.0'
            implementation 'javax.servlet:javax.servlet-api:3.1.0'
        
            implementation 'io.prometheus:simpleclient:0.6.0'
            implementation 'io.prometheus:simpleclient_hotspot:0.6.0'
            implementation 'io.prometheus:simpleclient_servlet:0.6.0'
        } 
        
        repositories {
            mavenCentral()
        }
        """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
        plugins {
            id 'java-library'
            id 'com.intershop.icm.cartridge'
        }
        
        dependencies {
            implementation "ch.qos.logback:logback-core:\${project.ext.logback_logback_classic_version}"
            implementation "ch.qos.logback:logback-classic:\${project.ext.logback_logback_classic_version}"
        } 
        
        repositories {
            mavenCentral()
        }        
        """.stripIndent())

        writeJavaTestClass("com.intershop.testCartridge1", prj1dir)
        writeJavaTestClass("com.intershop.testCartridge2", prj2dir)

        when:
        File gradleProps = file("gradle.properties")
        gradleProps.createNewFile()
        gradleProps << """
        inject_guice_version = 4.0
        logback_logback_classic_version = 1.2.3
        """.stripIndent()

        def result = getPreparedGradleRunner()
                .withArguments("copyThirdpartyLibs")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:copyThirdpartyLibs').outcome == SUCCESS
        result.task(':testCartridge2:copyThirdpartyLibs').outcome == SUCCESS

        new File(testProjectDir,"testCartridge2/build/lib/ch.qos.logback-logback-classic-1.2.3.jar").exists()
        new File(testProjectDir,"testCartridge1/build/lib/com.google.inject-guice-4.0.jar").exists()

        when:
        gradleProps.delete()
        gradleProps.createNewFile()
        gradleProps << """
        inject_guice_version = 4.2.2
        logback_logback_classic_version = 1.2.0
        """.stripIndent()

        def result2 = getPreparedGradleRunner()
                .withArguments("copyThirdpartyLibs")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':testCartridge1:copyThirdpartyLibs').outcome == SUCCESS
        result2.task(':testCartridge2:copyThirdpartyLibs').outcome == SUCCESS

        new File(testProjectDir,"testCartridge2/build/lib/ch.qos.logback-logback-classic-1.2.0.jar").exists()
        new File(testProjectDir,"testCartridge1/build/lib/com.google.inject-guice-4.2.2.jar").exists()
        ! new File(testProjectDir,"testCartridge2/build/lib/ch.qos.logback-logback-classic-1.2.3.jar").exists()
        ! new File(testProjectDir,"testCartridge1/build/lib/com.google.inject-guice-4.0.jar").exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Simple test of WriteCartridgeClasspath'(){
        given:
        File gradleProperties = new File(testProjectDir, "gradle.properties")
        gradleProperties << """
            classpath.file.enabled = true
        """

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            buildDir = new File(projectDir, 'target')
            
            version = "1.0.0"
            
            repositories {
                mavenCentral()
            }
            """.stripIndent()

            def prj1dir = createSubProject('testCartridge1', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
            
            buildDir = new File(projectDir, 'target')
            
            dependencies {
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
            
            repositories {
                mavenCentral()
            }
            """.stripIndent())

            def prj2dir = createSubProject('testCartridge2', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
                
            buildDir = new File(projectDir, 'target')
            
            dependencies {
                cartridge project(':testCartridge1')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

            def prj3dir = createSubProject('testCartridge3', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }

            buildDir = new File(projectDir, 'target')

            dependencies {
                cartridge project(':testCartridge2')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("writeCartridgeClasspath", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:writeCartridgeClasspath').outcome == SUCCESS
        result.task(':testCartridge2:writeCartridgeClasspath').outcome == SUCCESS
        result.task(':testCartridge3:writeCartridgeClasspath').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Simple test of WriteCartridgeDescriptor'(){
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            buildDir = new File(projectDir, 'target')
            
            version = "1.0.0"

            repositories {
                mavenCentral()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
            
            description = "Test cartridge implementation"    
            
            buildDir = new File(projectDir, 'target')
            
            dependencies {
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
            
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            } 
            
            buildDir = new File(projectDir, 'target')
            
            dependencies {
                cartridge project(':testCartridge1')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        def prj3dir = createSubProject('testCartridge3', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
            
            description = \"""\\
            Test cartridge implementation first line
            Test cartridge implementation second line   
            \"""

            buildDir = new File(projectDir, 'target')
             
            dependencies {
                cartridge project(':testCartridge2')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("writeCartridgeDescriptor", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:writeCartridgeDescriptor').outcome == SUCCESS
        result.task(':testCartridge2:writeCartridgeDescriptor').outcome == SUCCESS
        result.task(':testCartridge3:writeCartridgeDescriptor').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions

    }

    def 'Extended test of WriteCartridgeDescriptor'(){
        given:
        File gradleProperties = new File(testProjectDir, "gradle.properties")
        gradleProperties << """
            classpath.file.enabled = true
        """

        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm.base'
            }
            
            version = "1.0.0"

            repositories {
                mavenCentral()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
            
            group = 'com.intershop'
            
            dependencies {
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
            
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
            
            group = 'com.intershop'
            
            dependencies {
                cartridge project(':testCartridge1')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        def prj3dir = createSubProject('testCartridge3', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
            
            group = 'com.intershop'

            dependencies {
                cartridge project(':testCartridge2')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        def prj4dir = createSubProject('testCartridge4', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
                
            group = 'com.intershop'
            
            dependencies {
                cartridge project(':testCartridge3')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'

                runtime 'net.logstash.logback:logstash-logback-encoder:4.11'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        def prj5dir = createSubProject('testCartridge5', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
             
            group = 'com.intershop'
            
            writeCartridgeClasspath.useClassesFolder = true
             
            dependencies {
                cartridge project(':testCartridge3')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
                
                implementation 'org.hamcrest:hamcrest-library:2.1'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        writeJavaTestClass("com.intershop.test", prj5dir)

        def prj6dir = createSubProject('testCartridge6', """
            plugins {
                id 'java-library'
                id 'com.intershop.icm.cartridge'
            }
             
            group = 'com.intershop'
             
            dependencies {
                cartridge project(':testCartridge3')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                mavenCentral()
            }
            """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("writeCartridgeDescriptor", "writeCartridgeClasspath")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:writeCartridgeDescriptor').outcome == SUCCESS
        result.task(':testCartridge2:writeCartridgeDescriptor').outcome == SUCCESS
        result.task(':testCartridge3:writeCartridgeDescriptor').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions

    }

    @Ignore
    def 'Extended test of WriteCartridgeDescriptor with platform dependencies'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.icm.cartridge'
            }
            
            group = 'com.intershop.test'
            
            dependencies {
                cartridge enforcedPlatform("com.intershop.icm:versions:7.11.0.0-IS-29326_add_test_components_for_publishing-SNAPSHOT")
            
                cartridge "com.intershop.business:bc_mvc"
                cartridge "com.intershop.content:bc_pmc"
                cartridge "com.intershop.business:bc_product_rating_orm"
                cartridge "com.intershop.business:bc_search"
                cartridge "com.intershop.business:bc_catalog"
                cartridge "com.intershop.platform:bc_service"
             
                cartridge "com.intershop.platform:app"
                cartridge "com.intershop.business:bc_image"
                cartridge "com.intershop.platform:core"
                cartridge "com.intershop.platform:configuration"
                cartridge "com.intershop.platform:jmx"
                cartridge "com.intershop.platform:cache"
                cartridge "com.intershop.platform:orm"
                cartridge "com.intershop.business:xcs"
                cartridge "com.intershop.platform:pf_objectgraph_guice"
                cartridge "com.intershop.platform:bc_application"
                cartridge "com.intershop.platform:businessobject"
                cartridge "com.intershop.platform:bc_repository"
                cartridge "com.intershop.platform:bc_foundation"
                cartridge "com.intershop.platform:pf_objectgraph"
                
                implementation "com.google.guava:guava"
                implementation "com.google.inject.extensions:guice-assistedinject"
            }
            
            repositories {
                maven {
                    url = uri("https://repo.rnd.intershop.de/local-mvn-snapshots/")
                }
                maven {
                    url = uri("https://repo.rnd.intershop.de/mvn-internal/")
                }
            }
        """.stripIndent()

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments("writeCartridgeClasspath", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':writeCartridgeClasspath').outcome == SUCCESS

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("writeCartridgeDescriptor", "writeCartridgeClasspath", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':writeCartridgeDescriptor').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'ZipFile will be created and published'() {

        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'maven-publish'
                id 'com.intershop.gradle.icm.base'
                id 'com.intershop.icm.cartridge.adapter'
            }

            def publishDir = "\$buildDir/repo"  

            group = "com.intershop"
            version = "1.0.0"

            subprojects {
                apply plugin: 'maven-publish'
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
                    id 'com.intershop.icm.cartridge.adapter'
                    id 'java'
                }
                """.stripIndent())

        writeJavaTestClass("com.intershop.test.Test1", prjDir1)
        file("staticfiles/cartridge/logback/logback-test.xml", prjDir1)

        File prjDir2 = createSubProject(":subprj2",
                """
                plugins {
                    id 'com.intershop.icm.cartridge.adapter'
                    id 'java'
                }
                """.stripIndent())

        writeJavaTestClass("com.intershop.test.Test2", prjDir2)
        file("staticfiles/cartridge/test/test.properties", prjDir2)

        when:
        def result = getPreparedGradleRunner()
                .withArguments("publish", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':subprj1:publish').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Run complete configuration'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.base' version '{latestRevision}'
            }
            
            group = "com.intershop.test"
            version = "7.11.0.0"
            
            intershop {
            
                projectInfo {
                    productID = 'ICM 7 B2C'
                    productName = 'Intershop Commerce Management 7 B2C'
                    copyrightOwner = 'Intershop Communications'
                    copyrightFrom = '2005'
                    organization = 'Intershop Communications'
                }
            
                mavenPublicationName = 'ishmvn'
            }
            
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
            id 'java'
            id 'com.intershop.icm.cartridge.product'
        }
        
        description = "Test cartridge implementation"    

        dependencies {
            implementation 'com.google.inject:guice:4.0'
            implementation 'com.google.inject.extensions:guice-servlet:3.0'
            implementation 'javax.servlet:javax.servlet-api:3.1.0'
        
            implementation 'io.prometheus:simpleclient:0.6.0'
            implementation 'io.prometheus:simpleclient_hotspot:0.6.0'
            implementation 'io.prometheus:simpleclient_servlet:0.6.0'
        } 
        
        repositories {
            mavenCentral()
        }
        
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = "\${project.rootProject.buildDir}/pubrepo"
                }
            }
        }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_test', """
        plugins {
            id 'java'
            id 'com.intershop.icm.cartridge.test'
        }
        
        description = "Test cartridge implementation"    
                
        dependencies {
            implementation 'org.codehaus.janino:janino:2.5.16'
            implementation 'org.codehaus.janino:commons-compiler:3.0.6'
            implementation 'ch.qos.logback:logback-core:1.2.3'
            implementation 'ch.qos.logback:logback-classic:1.2.3'
            implementation 'commons-collections:commons-collections:3.2.2'
        } 
        
        repositories {
            mavenCentral()
        } 
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            id 'java'
            id 'com.intershop.icm.cartridge.development'
        }
        
        description = "Test cartridge implementation"    

        repositories {
            mavenCentral()
        }    
        
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = "\${project.rootProject.buildDir}/pubrepo"
                }
            }
        }    
        """.stripIndent())

        def prj4dir = createSubProject('prjCartridge_adapter', """
        plugins {
            id 'java'
            id 'com.intershop.icm.cartridge.adapter'
        }

        description = "Test cartridge implementation"        

        dependencies {
            implementation 'ch.qos.logback:logback-core:1.2.3'
            implementation 'ch.qos.logback:logback-classic:1.2.3'
        } 
        
        repositories {
            mavenCentral()
        }     
        
        publishing {
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url = "\${project.rootProject.buildDir}/pubrepo"
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

        when:
        def resultPkgMain = getPreparedGradleRunner()
                .withArguments(CreateMainPackage.DEFAULT_NAME)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPkgMain.task(":${CreateMainPackage.DEFAULT_NAME}").outcome == SUCCESS
        file("build/packages/mainpkg.tgz").exists()

        when:
        def resultPkgTest = getPreparedGradleRunner()
                .withArguments(CreateTestPackage.DEFAULT_NAME)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPkgTest.task(":${CreateTestPackage.DEFAULT_NAME}").outcome == SUCCESS
        file("build/packages/testpkg.tgz").exists()

        when:
        def resultPkgInit = getPreparedGradleRunner()
                .withArguments(CreateInitPackage.DEFAULT_NAME)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPkgInit.task(":${CreateInitPackage.DEFAULT_NAME}").outcome == NO_SOURCE

        when:
        def resultPkgTestInit = getPreparedGradleRunner()
                .withArguments(CreateInitTestPackage.DEFAULT_NAME)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultPkgTestInit.task(":${CreateInitTestPackage.DEFAULT_NAME}").outcome == NO_SOURCE

        where:
        gradleVersion << supportedGradleVersions
    }
}
