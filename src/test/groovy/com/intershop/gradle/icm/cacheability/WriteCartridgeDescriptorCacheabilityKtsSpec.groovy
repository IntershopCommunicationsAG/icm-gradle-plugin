package com.intershop.gradle.icm.cacheability

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Integration tests to verify that the {@code writeCartridgeDescriptor} task
 * is properly build cache compatible.
 */
class WriteCartridgeDescriptorCacheabilityKtsSpec extends AbstractCacheabilitySpec {

    def 'writeCartridgeDescriptor task should be loaded from cache on second build'() {
        given:
        prepareSingleCartridgeBuild()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
                .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task executes successfully'
        result1.task(':writeCartridgeDescriptor').outcome == SUCCESS
        new File(testProjectDir, 'build/descriptor/cartridge.descriptor').exists()

        when: 'Clean and rebuild using the build cache'
        def result2 = getPreparedGradleRunner()
                .withArguments('clean', 'writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task output is restored from cache'
        result2.task(':writeCartridgeDescriptor').outcome == FROM_CACHE
        new File(testProjectDir, 'build/descriptor/cartridge.descriptor').exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'writeCartridgeDescriptor task should use cache across different project directories'() {
        given:
        prepareSingleCartridgeBuild()

        when: 'First build in the original project directory'
        def result1 = getPreparedGradleRunner()
                .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':writeCartridgeDescriptor').outcome == SUCCESS

        when: 'A second project is created in a different directory with identical content'
        def testProjectDir2 = Files.createTempDirectory(
                "gradle-test-project-${getClass().simpleName}-").toFile()
        testProjectDir2.deleteOnExit()
        copyDirectory(testProjectDir, testProjectDir2)

        and: 'Build in the new directory using the shared build cache'
        def result2 = getPreparedGradleRunner()
                .withProjectDir(testProjectDir2)
                .withArguments('clean', 'writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task output is restored from cache despite being in a different directory'
        result2.task(':writeCartridgeDescriptor').outcome == FROM_CACHE

        cleanup:
        testProjectDir2?.deleteDir()

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'writeCartridgeDescriptor task should produce a cache miss when cartridge version changes'() {
        given:
        prepareSingleCartridgeBuild()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
                .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':writeCartridgeDescriptor').outcome == SUCCESS

        when: 'The project version is changed (an input changes)'
        buildFile.text = buildFile.text.replace('version = "1.0.0"', 'version = "2.0.0"')

        and: 'Rebuild with the modified input'
        def result2 = getPreparedGradleRunner()
                .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task re-executes because an input changed'
        result2.task(':writeCartridgeDescriptor').outcome == SUCCESS

        when: 'Version is reverted to its original value'
        buildFile.text = buildFile.text.replace('version = "2.0.0"', 'version = "1.0.0"')

        and: 'Rebuild after clean – original inputs are back so cache entry must be found'
        def result3 = getPreparedGradleRunner()
                .withArguments('clean', 'writeCartridgeDescriptor', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task is loaded from cache because the original inputs are restored'
        result3.task(':writeCartridgeDescriptor').outcome == FROM_CACHE

        where:
        gradleVersion << supportedGradleVersions
    }
}

