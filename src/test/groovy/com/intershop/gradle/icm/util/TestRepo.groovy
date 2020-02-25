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
package com.intershop.gradle.icm.util

import com.intershop.gradle.test.builder.TestMavenRepoBuilder

class TestRepo {

    private File repoDir
    private String intRepoconfig

    TestRepo(File repoDirTarget) {
        repoDir = new File(repoDirTarget.absolutePath)

        if(repoDir.exists()) {
            repoDir.deleteDir()
            repoDir.mkdirs()
        }
    }

    String getRepoConfig() {
        if(! intRepoconfig) {
            intRepoconfig = createRepo(repoDir)
        }
        return intRepoconfig
    }

    private String createRepo(File dir) {

        dir.deleteDir()
        dir.mkdirs()

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId: 'libbom', version: '1.0.0') {
                dependencyManagement {
                    dependency groupId: 'com.other', artifactId: 'library1', version: '1.5.0'
                    dependency groupId: 'com.other', artifactId: 'library2', version: '1.5.0'
                    dependency groupId: 'com.other', artifactId: 'library3', version: '1.5.0'
                }
            }

            project(groupId: 'com.intershop', artifactId: 'library3', version: '1.0.0'){
                dependencyManagement {
                    dependency groupId: 'com.intershop', artifactId: 'libbom', version: '1.5.0'
                }
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class3/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class3/test2.file', content: 'test2.file'),
                ]
                dependency groupId: 'com.other', artifactId: 'library2', version: '1.5.0'
                dependency groupId: 'com.intershop', artifactId: 'library2', version: '1.0.0'
            }
            project(groupId: 'com.other', artifactId: 'library4', version: '1.0.0'){
                dependencyManagement {
                    dependency groupId: 'com.intershop', artifactId: 'libbom', version: '1.5.0'
                }
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class4/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class4/test2.file', content: 'test2.file'),
                ]
                dependency groupId: 'com.intershop', artifactId: 'library3', version: '1.0.0'
            }

        }.writeTo(repoDir)

        addSimpleLib('library1', '1.5.0')
        addSimpleLib('library2', '1.5.0')
        addSimpleLib('library3', '1.5.0')

        addExtLib('library1', '1.0.0', 'library1', '1.5.0')
        addExtLib('library2', '1.0.0', 'library2', '1.5.0')

        addCartridge('cartridge2', '1.0.0', 'development')

        String repostr = """
            repositories {
                jcenter()
                maven {
                    url "${repoDir.toURI().toURL()}"
                }
            }""".stripIndent()

        return repostr
    }

    private void addSimpleLib(String artifactId, String version) {
        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.other', artifactId: artifactId, version: version) {
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class1/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class1/test2.file', content: 'test2.file'),
                ]
            }
        }.writeTo(repoDir)
    }

    private void addExtLib(String artifactId, String version, String depArtifactId, String depVersion) {
        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId: artifactId, version: version) {
                dependencyManagement {
                    dependency groupId: 'com.intershop', artifactId: 'libbom', version: '1.5.0'
                }
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class1/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class1/test2.file', content: 'test2.file'),
                ]
                dependency groupId: 'com.other', artifactId: depArtifactId, version: depVersion
            }
        }.writeTo(repoDir)
    }

    private void addCartridge(String artifactId, String version, String style) {
        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.cartridge', artifactId: artifactId, version: version) {
                dependencyManagement {
                    dependency groupId: 'com.intershop', artifactId: 'libbom', version: '1.5.0'
                }
                mvnProperties pairs: [ 'cartridge.name': artifactId, 'cartridge.style': style, 'cartridge.type':'external']
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/cclass1/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/cclass1/test2.file', content: 'test1.file')
                ]
                artifact (
                        classifier: "staticfiles",
                        ext: "zip",
                        entries: [
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'components/test1.component', content: 'text = test1.component'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'localozations/cartridge_de.properties', content: 'text = test1_de'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'localozations/cartridge_en.properties', content: 'text = test1_en'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'pagecompile/default/template1.jsp', content: 'jsp content file'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'pagecompile/org/apache/jsp/cartridge/default/template1.jsp', content: 'jsp content file'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'pagecompile/org/apache/jsp/cartridge/default/template1.class', content: 'class content file'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'pipelines/pipeline1.pipeline', content: 'pipeline content file'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'pipelines/pipelines-acl.properties', content: 'test = value'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'templates/default/template1.isml', content: 'isml content file'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'webforms/webform1.properties', content: 'webbform = webform1'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'webforms/webform1.webform', content: 'webbform content file'),
                                TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'cartridge.descriptor', content: 'cartridge content file')
                        ]
                )
                dependency groupId: 'com.other', artifactId: 'library2', version: '1.5.0'
                dependency groupId: 'com.other', artifactId: 'library3', version: '1.5.0'
            }
        }.writeTo(repoDir)
    }
}