package com.intershop.gradle.icm.cacheability

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Integration tests to verify that the {@code createConfigProd} task
 * (backed by {@code CreateConfigFolder}) is properly build cache compatible.
 *
 * The task name {@code createConfigProd} is defined by
 * {@code TaskName.PRODUCTION.config()} in the plugin source.
 */
class CreateConfigFolderCacheabilityKtsSpec extends AbstractCacheabilitySpec {

    def 'createConfigProd task should be loaded from cache on second build'() {
        given:
        prepareMultiProjectBuild()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
                .withArguments('createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task executes successfully'
        result1.task(':createConfigProd').outcome == SUCCESS

        when: 'Clean and rebuild using the build cache'
        def result2 = getPreparedGradleRunner()
                .withArguments('clean', 'createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task output is restored from cache'
        result2.task(':createConfigProd').outcome == FROM_CACHE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'createConfigProd task should produce a cache miss when a createServerInfo input changes'() {
        given:
        prepareMultiProjectBuild()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
                .withArguments('createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':createConfigProd').outcome == SUCCESS

        when: 'A declared @Input of createServerInfo is changed, so versionInfo content changes'
        buildFile.text = buildFile.text.replace(
                'productName.set("Test Product Name")',
                'productName.set("Modified Product Name")')

        and: 'Rebuild with the modified input'
        def result2 = getPreparedGradleRunner()
                .withArguments('createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task re-executes because the versionInfo @InputFile content changed'
        result2.task(':createConfigProd').outcome == SUCCESS

        when: 'productName is reverted to its original value'
        buildFile.text = buildFile.text.replace(
                'productName.set("Modified Product Name")',
                'productName.set("Test Product Name")')

        and: 'Rebuild after clean – original inputs are back'
        def result3 = getPreparedGradleRunner()
                .withArguments('clean', 'createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task is loaded from cache because the original inputs are restored'
        result3.task(':createConfigProd').outcome == FROM_CACHE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'createConfigProd task should use cache across different project directories'() {
        given:
        prepareMultiProjectBuild()

        when: 'First build in the original project directory'
        def result1 = getPreparedGradleRunner()
                .withArguments('createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':createConfigProd').outcome == SUCCESS

        when: 'A second project is created in a different directory with identical content'
        def testProjectDir2 = Files.createTempDirectory(
                "gradle-test-project-${getClass().simpleName}-").toFile()
        testProjectDir2.deleteOnExit()
        copyDirectory(testProjectDir, testProjectDir2)

        and: 'Build in the new directory using the shared build cache'
        def result2 = getPreparedGradleRunner()
                .withProjectDir(testProjectDir2)
                .withArguments('clean', 'createConfigProd', '--build-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task output is restored from cache despite being in a different directory'
        result2.task(':createConfigProd').outcome == FROM_CACHE

        cleanup:
        testProjectDir2?.deleteDir()

        where:
        gradleVersion << supportedGradleVersions
    }
}
