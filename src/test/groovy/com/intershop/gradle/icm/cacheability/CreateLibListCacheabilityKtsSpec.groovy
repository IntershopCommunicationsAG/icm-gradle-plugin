package com.intershop.gradle.icm.cacheability

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Integration tests to verify that the {@code productionCreateLibraries} task
 * (backed by {@code CreateLibList}) is properly build cache compatible.
 */
class CreateLibListCacheabilityKtsSpec extends AbstractCacheabilitySpec {

    def 'productionCreateLibraries task should be loaded from cache on second build'() {
        given:
        prepareMultiProjectBuild()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
                .withArguments('productionCreateLibraries', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task executes successfully'
        result1.task(':productionCreateLibraries').outcome == SUCCESS

        when: 'Clean and rebuild using the build cache'
        def result2 = getPreparedGradleRunner()
                .withArguments('clean', 'productionCreateLibraries', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task output is restored from cache'
        result2.task(':productionCreateLibraries').outcome == FROM_CACHE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'productionCreateLibraries task should use cache across different project directories'() {
        given:
        prepareMultiProjectBuild()

        when: 'First build in the original project directory'
        def result1 = getPreparedGradleRunner()
                .withArguments('productionCreateLibraries', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':productionCreateLibraries').outcome == SUCCESS

        when: 'A second project is created in a different directory with identical content'
        def testProjectDir2 = Files.createTempDirectory(
                "gradle-test-project-${getClass().simpleName}-").toFile()
        testProjectDir2.deleteOnExit()
        copyDirectory(testProjectDir, testProjectDir2)

        and: 'Build in the new directory using the shared build cache'
        def result2 = getPreparedGradleRunner()
                .withProjectDir(testProjectDir2)
                .withArguments('clean', 'productionCreateLibraries', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task output is restored from cache despite being in a different directory'
        result2.task(':productionCreateLibraries').outcome == FROM_CACHE

        cleanup:
        testProjectDir2?.deleteDir()

        where:
        gradleVersion << supportedGradleVersions
    }
}

