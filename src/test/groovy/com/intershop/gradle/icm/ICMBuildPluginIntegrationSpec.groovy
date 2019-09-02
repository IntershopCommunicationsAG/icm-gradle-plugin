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

import com.intershop.gradle.test.AbstractIntegrationGroovySpec
import org.gradle.testkit.runner.TaskOutcome

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ICMBuildPluginIntegrationSpec extends AbstractIntegrationGroovySpec {

    def 'Plugin is applied to the root project'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm'
            }
        """.stripIndent()

        createSubProject(":subprj1",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        createSubProject(":subprj2",
                """
                plugins {
                    id 'java'
                }
                """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("projects")
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
                    id 'java'
                    id 'com.intershop.gradle.icm'
                }
                """.stripIndent())

        createSubProject(":subprj2",
                """
                plugins {
                    id 'java'
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

    def 'CreateServerInfoProperties is configured and works with default'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm'
            }
            
            version = '1.0.0'

            intershop {
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments("createServerInfoProperties")
                .withGradleVersion(gradleVersion)
                .build()

        File f = new File(testProjectDir, "build/serverInfoProps/version.properties")
        Properties props = new Properties()
        props.load(f.newDataInputStream())

        then:
        result.task(':createServerInfoProperties').outcome == SUCCESS
        props.getProperty("version.information.productName") == "Intershop Commerce Management 7"

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'CreateServerInfoProperties is configured and works with changed configuration'() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm'
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
                .withArguments("createServerInfoProperties")
                .withGradleVersion(gradleVersion)
                .build()

        File f = new File(testProjectDir, "build/serverInfoProps/version.properties")
        Properties props = new Properties()
        props.load(f.newDataInputStream())

        then:
        result.task(':createServerInfoProperties').outcome == SUCCESS
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
                id 'com.intershop.gradle.icm'
            }
            
            repositories {
                jcenter()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
        apply plugin: 'java'
        
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
        apply plugin: 'java'
        
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
                .withArguments("copyThirdpartyLibs")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:copyThirdpartyLibs').outcome == SUCCESS
        result.task(':testCartridge2:copyThirdpartyLibs').outcome == SUCCESS

        file("testCartridge2/build/lib/ch.qos.logback-logback-classic-1.2.3.jar").exists()
        file("testCartridge1/build/lib/com.google.inject-guice-4.0.jar").exists()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("copyThirdpartyLibs")
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
                id 'com.intershop.gradle.icm'
            }
            
            repositories {
                jcenter()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
        apply plugin: 'java'
        
        dependencies {
            implementation "com.google.inject:guice:\${project.ext.inject_guice_version}"
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
        apply plugin: 'java'
        
        dependencies {
            implementation "ch.qos.logback:logback-core:\${project.ext.logback_logback_classic_version}"
            implementation "ch.qos.logback:logback-classic:\${project.ext.logback_logback_classic_version}"
        } 
        
        repositories {
            jcenter()
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
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm'
            }
            
            version = "1.0.0"
            
            repositories {
                jcenter()
            }
            """.stripIndent()

            def prj1dir = createSubProject('testCartridge1', """
            plugins {
                id 'java'
            }
            
            dependencies {
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
            
            repositories {
                jcenter()
            }
            """.stripIndent())

            def prj2dir = createSubProject('testCartridge2', """
            plugins {
                id 'java'
            }
                
            dependencies {
                cartridge project(':testCartridge1')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

            def prj3dir = createSubProject('testCartridge3', """
            plugins {
                id 'java'
            }

            dependencies {
                cartridge project(':testCartridge2')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
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
                id 'com.intershop.gradle.icm'
            }
            
            version = "1.0.0"
            
            repositories {
                jcenter()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
            plugins {
                id 'java'
            }
            
            dependencies {
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
            
            repositories {
                jcenter()
            }
            """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
            plugins {
                id 'java'
            }
            
            dependencies {
                cartridge project(':testCartridge1')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

        def prj3dir = createSubProject('testCartridge3', """
            plugins {
                id 'java'
            }
             
            dependencies {
                cartridge project(':testCartridge2')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("writeCartridgeDescriptor")
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
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.icm'
            }
            
            version = "1.0.0"
            
            repositories {
                jcenter()
            }
            """.stripIndent()

        def prj1dir = createSubProject('testCartridge1', """
            plugins {
                id 'java'
            }
            
            dependencies {
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
            
            repositories {
                jcenter()
            }
            """.stripIndent())

        def prj2dir = createSubProject('testCartridge2', """
            plugins {
                id 'java'
            }
            
            dependencies {
                cartridge project(':testCartridge1')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

        def prj3dir = createSubProject('testCartridge3', """
            plugins {
                id 'java'
            }

            dependencies {
                cartridge project(':testCartridge2')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

        def prj4dir = createSubProject('testCartridge4', """
            plugins {
                id 'java'
            }
                
            dependencies {
                cartridge project(':testCartridge3')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

        def prj5dir = createSubProject('testCartridge5', """
            plugins {
                id 'java'
            }
             
            dependencies {
                cartridge project(':testCartridge3')
                implementation "com.google.inject:guice:4.0"
                implementation 'com.google.inject.extensions:guice-servlet:3.0'
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
            } 
                
            repositories {
                jcenter()
            }
            """.stripIndent())

        when:
        def result = getPreparedGradleRunner()
                .withArguments("writeCartridgeDescriptor")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':testCartridge1:writeCartridgeDescriptor').outcome == SUCCESS
        result.task(':testCartridge2:writeCartridgeDescriptor').outcome == SUCCESS
        result.task(':testCartridge3:writeCartridgeDescriptor').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions

    }
}
