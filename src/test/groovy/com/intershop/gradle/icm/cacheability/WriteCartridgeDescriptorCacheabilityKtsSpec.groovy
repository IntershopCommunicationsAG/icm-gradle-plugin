package com.intershop.gradle.icm.cacheability

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Integration tests to verify that the {@code writeCartridgeDescriptor} task
 * is properly build cache and configuration cache compatible.
 */
class WriteCartridgeDescriptorCacheabilityKtsSpec extends AbstractCacheabilitySpec {

    // -------------------------------------------------------------------------
    // Build-cache compatibility
    // -------------------------------------------------------------------------

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

    def 'writeCartridgeDescriptor task should produce a cache miss when cartridgeDependsOn changes'() {
        given:
        prepareSingleCartridgeBuildWithDependencies()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
            .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
            .withGradleVersion(gradleVersion)
            .build()

        then:
        result1.task(':writeCartridgeDescriptor').outcome == SUCCESS

        when: 'A second external cartridge dependency is added, changing the cartridgeDependsOn @Input'
        buildFile.text = buildFile.text.replace(
            'add("cartridge", "com.intershop.cartridge:cartridge_prod:1.0.0")',
            '''add("cartridge", "com.intershop.cartridge:cartridge_prod:1.0.0")
               add("cartridge", "com.intershop.cartridge:cartridge_dev:1.0.0")'''
        )

        and: 'Task is re-run against the build cache'
        def result2 = getPreparedGradleRunner()
            .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
            .withGradleVersion(gradleVersion)
            .build()

        then: 'Task re-executes (cache miss) because cartridgeDependsOn changed'
        result2.task(':writeCartridgeDescriptor').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'writeCartridgeDescriptor task should produce a cache miss when cartridgeDependsOnLibs changes'() {
        given:
        prepareSingleCartridgeBuildWithDependencies()

        when: 'First build populates the build cache'
        def result1 = getPreparedGradleRunner()
            .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
            .withGradleVersion(gradleVersion)
            .build()

        then:
        result1.task(':writeCartridgeDescriptor').outcome == SUCCESS

        when: 'An additional library dependency is added, changing the cartridgeDependsOnLibs @Input'
        buildFile.text = buildFile.text.replace(
            'implementation("com.other:library1:1.5.0")',
            '''implementation("com.other:library1:1.5.0")
               implementation("com.other:library4:1.0.0")'''
        )

        and: 'Task is re-run against the build cache'
        def result2 = getPreparedGradleRunner()
            .withArguments('writeCartridgeDescriptor', '--build-cache', '-s')
            .withGradleVersion(gradleVersion)
            .build()

        then: 'Task re-executes (cache miss) because cartridgeDependsOnLibs changed'
        result2.task(':writeCartridgeDescriptor').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    // -------------------------------------------------------------------------
    // Configuration-cache compatibility
    // -------------------------------------------------------------------------

    def 'writeCartridgeDescriptor task should be compatible with the configuration cache'() {
        given:
        prepareSingleCartridgeBuild()

        when: 'First build stores the configuration-cache entry'
        def result1 = getPreparedGradleRunner()
                .withArguments('writeCartridgeDescriptor', '--configuration-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'Task executes successfully'
        result1.task(':writeCartridgeDescriptor').outcome == SUCCESS
        !result1.output.contains('not supported with the configuration cache')

        when: 'Second build runs with the same inputs'
        def result2 = getPreparedGradleRunner()
                .withArguments('writeCartridgeDescriptor', '--configuration-cache', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then: 'The configuration-cache entry is reused'
        result2.output.toLowerCase().contains('reusing configuration cache')

        where:
        gradleVersion << supportedGradleVersions
    }
}

