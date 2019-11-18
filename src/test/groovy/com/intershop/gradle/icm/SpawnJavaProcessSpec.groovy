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
import spock.lang.Ignore

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS


class SpawnJavaProcessSpec extends AbstractIntegrationGroovySpec {

    def "process will be started by task"() {
        given:
        settingsFile << """
        rootProject.name='rootproject'
        """.stripIndent()

        def prj1dir = createSubProject(":subprj1",
                """
                plugins {
                    id 'java-library'
                }
                """.stripIndent())

        def prj2dir = createSubProject(":subprj2",
                """
                plugins {
                    id 'java-library'
                }
                """.stripIndent())

        writeJavaClass("com.intershop.beehive.startup", "ServletEngineStartup", prj1dir)
        writeJavaClass("com.intershop.beehive.runtime", "EnfinitySystemClassLoader", prj2dir)

        buildFile << """
            plugins {
                id 'com.intershop.gradle.icm.base'
            }
            
            configurations.create('spawntestproc')
            
            dependencies {
                spawntestproc project(':subprj1')
                spawntestproc project(':subprj2')
            }
            
            task startTestServer(type: com.intershop.gradle.icm.tasks.SpawnJavaProcess) {
            
                group = "Test Tasks"
                description = "Spawn test process"
                
                main = "com.intershop.beehive.startup.ServletEngineStartup"
                readyString = "process started"
                pidFile = new File(project.buildDir, "activeServer/server.pid")
                
                classpath = project.configurations.findByName( "spawntestproc" ) 
            }
        """.stripIndent()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments("startTestServer", "-s")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':startTestServer').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    protected File writeJavaClass(String packageDotted, String className, File baseDir = testProjectDir) {
        String path = 'src/main/java/' + packageDotted.replace('.', '/') + '/' + className + '.java'
        File javaFile = file(path, baseDir)
        javaFile << """package ${packageDotted};
            public class ${className} {
                public static void main(String[] args) throws InterruptedException {
                    System.out.println("Hello Integration Test");
            
                    int i = 0;
                    while(i < 100) {
                        ++i;
                        if(i == 50) {
                            System.out.println("time stamp ... process started in 900 sec");
                        }
                        Thread.sleep(1000);
                    }
                }
            }
        """.stripIndent()

        return javaFile
    }
}
