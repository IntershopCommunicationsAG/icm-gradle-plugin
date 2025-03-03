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
import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ICMProjectPluginIntegrationKotlinSpec extends AbstractIntegrationKotlinSpec {
    public final static String ENV_GRADLE_USER_HOME = "GRADLE_USER_HOME"

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
        result.output.contains(new File(getGradleUserHome(), "icm-default/conf/icm.properties").toString())

        when:
        def confDir1 = new File(getUserHome(), "conf")
        def result1 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-DconfigDir=$confDir1")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':showConfPath').outcome == SUCCESS
        result1.output.contains(new File(confDir1, "icm.properties").toString())

        when:
        def confDir2 = new File(getUserHome().parentFile, "otheruser/conf")
        def result2 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all", "-PconfigDir=$confDir2")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':showConfPath').outcome == SUCCESS
        result2.output.contains(new File(confDir2, "icm.properties").toString())

        when:
        def confDir3 = new File(getUserHome().parentFile, "otheruser/conf")
        def result3 = getPreparedGradleRunner()
                .withArguments("showConfPath", "-s", "--warning-mode", "all")
                .withEnvironment([ "CONFIGDIR": confDir3.toString() ])
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':showConfPath').outcome == SUCCESS
        result3.output.contains(new File(confDir3, "icm.properties").toString())

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
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri(project.rootProject.layout.buildDirectory.dir("pubrepo").get())
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
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        return repoConf
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
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri(project.rootProject.layout.buildDirectory.dir("pubrepo").get())
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
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        return repoConf
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
                }
            }

            ${repoConf}

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri(project.rootProject.layout.buildDirectory.dir("pubrepo").get())
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
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        return repoConf
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
                        url = uri(project.rootProject.layout.buildDirectory.dir("pubrepo").get())
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
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_dev', """
        plugins {
            `java`        
            id("com.intershop.icm.cartridge.development")
        }
        
        repositories {
            mavenCentral()
            gradlePluginPortal()
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
            gradlePluginPortal()
        }
        """.stripIndent())

        writeJavaTestClass("com.intershop.prod", prj1dir)
        writeJavaTestClass("com.intershop.test", prj2dir)
        writeJavaTestClass("com.intershop.dev", prj3dir)
        writeJavaTestClass("com.intershop.adapter", prj4dir)

        return repoConf
    }

    @Override
    protected GradleRunner getPreparedGradleRunner() {
        def runner = super.getPreparedGradleRunner()
        runner.withEnvironment([(ENV_GRADLE_USER_HOME): getGradleUserHome().toString()])
        return runner
    }

    private File getGradleUserHome() {
        String gradleUserHomeStr = System.getenv(ENV_GRADLE_USER_HOME)
        if (gradleUserHomeStr != null){
            return new File(gradleUserHomeStr)
        }

        return getUserHome()
    }

    private File getUserHome() {
        return new File(System.getProperty("user.home"), ".gradle")
    }
}
