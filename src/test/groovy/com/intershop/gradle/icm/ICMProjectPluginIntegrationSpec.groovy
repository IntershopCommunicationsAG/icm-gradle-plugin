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
import com.intershop.gradle.test.builder.TestMavenRepoBuilder

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ICMProjectPluginIntegrationSpec extends AbstractIntegrationGroovySpec {

    def setup() {
        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.platform.lib', artifactId: 'runtime-lib', version: '1.0.0-dev2') {
                artifact(ext: 'dylib', classifier: 'darwin', content: 'testfile1')
                artifact(ext: 'setpgid', classifier: 'darwin', content: 'testfile2')
                artifact(ext: 'so', classifier: 'linux', content: 'testfile3')
                artifact(ext: 'setpgid', classifier: 'linux', content: 'testfile4')
                artifact(ext: 'dll', classifier: 'win32', content: 'testfile5')
            }
        }.writeTo(new File(testProjectDir, "/repo"))
    }

}
