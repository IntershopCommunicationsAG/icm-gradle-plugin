package com.intershop.gradle.icm.cacheability

import com.intershop.gradle.icm.util.TestRepo
import com.intershop.gradle.test.AbstractIntegrationKotlinSpec

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Abstract base class for all ICM build cache integration tests.
 *
 * Provides:
 *  - a per-test temporary local build cache directory
 *  - helpers to set up a single-cartridge project build
 *  - helpers to set up a full multi-cartridge project build
 *  - a utility to copy a project tree into a different directory
 *    (used to verify path-insensitive cache hits)
 */
abstract class AbstractCacheabilitySpec extends AbstractIntegrationKotlinSpec {

    /** Unique, temporary build cache directory created fresh for every test method. */
    File tmpBuildCacheDir

    def setup() {
        tmpBuildCacheDir = Files.createTempDirectory("gradle-build-cache-${getClass().simpleName}-").toFile()
    }

    def cleanup() {
        tmpBuildCacheDir?.deleteDir()
    }

    // -------------------------------------------------------------------------
    // Build-setup helpers
    // -------------------------------------------------------------------------

    /**
     * Writes the settings file block that points the local build cache at
     * {@link #tmpBuildCacheDir}. Call this from every {@code prepareSingleCartridgeBuild}
     * / {@code prepareMultiProjectBuild} implementation so that each concrete spec
     * uses its own isolated cache directory.
     */
    protected String buildCacheBlock() {
        return """
            buildCache {
                local {
                    directory = file("${tmpBuildCacheDir.absolutePath.replace('\\', '\\\\')}")
                }
            }
        """.stripIndent()
    }

    /**
     * Sets up a minimal single-cartridge project build using the
     * {@code com.intershop.icm.cartridge.product} plugin.
     * A static HTML file is placed under {@code staticfiles/cartridge/pages/} so that
     * the {@code zipStaticFiles} task has at least one input file.
     */
    protected void prepareSingleCartridgeBuild() {
        settingsFile.text = """
            ${buildCacheBlock()}
            rootProject.name = "testCartridge"
        """.stripIndent()

        buildFile.text = """
            plugins {
                `java`
                id("com.intershop.icm.cartridge.product")
            }

            group = "com.intershop.test"
            version = "1.0.0"

            repositories {
                mavenCentral()
            }
        """.stripIndent()

        writeJavaTestClass("com.intershop.test", testProjectDir)

        def staticFile = new File(testProjectDir, "staticfiles/cartridge/text/test.txt")
        staticFile.parentFile.mkdirs()
        staticFile.text = "Test"
    }

    /**
     * Sets up a full multi-cartridge project build using the
     * {@code com.intershop.gradle.icm.project} plugin with three sub-projects
     * (test, development, adapter cartridges) backed by a local Maven
     * repository created via {@link TestRepo}.
     */
    protected void prepareMultiProjectBuild() {
        TestRepo repo = new TestRepo(new File(testProjectDir, "/repo"))
        String repoConf = repo.getRepoKtsConfig()

        settingsFile.text = """
            ${buildCacheBlock()}
            rootProject.name = "rootproject"
            include("prjCartridge_prod")
            include("prjCartridge_test")
            include("prjCartridge_dev")
            include("prjCartridge_adapter")
        """.stripIndent()

        createLocalFile("config/base/cluster/test.properties", "test.properties = base_dir")
        createLocalFile("config/prod/cluster/test.properties", "test.properties = prod_dir")
        createLocalFile("config/test/cluster/test.properties", "test_test = 1")
        createLocalFile("config/dev/cluster/test.properties",  "dev_test = 1")

        buildFile.text = """
            plugins {
                `java`
                id("com.intershop.gradle.icm.project")
            }

            group = "com.intershop.test"
            version = "1.0.0"

            intershop {
                projectInfo {
                    productID.set("Test Product ID")
                    productName.set("Test Product Name")
                    copyrightOwner.set("Intershop Communications")
                    copyrightFrom.set("2026")
                    organization.set("Intershop Communications")
                }
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

        def prj0dir = createSubProject('prjCartridge_prod', """
            plugins {
                `java`
                id("com.intershop.icm.cartridge.product")
            }
            dependencies {
                implementation("com.google.inject:guice:4.0")
                implementation("com.google.inject.extensions:guice-servlet:3.0")
                implementation("javax.servlet:javax.servlet-api:3.1.0")
            }
            repositories {
                mavenCentral()
            }
        """.stripIndent())

        def prj1dir = createSubProject('prjCartridge_test', """
            plugins {
                `java`
                id("com.intershop.icm.cartridge.test")
            }
            dependencies {
                implementation("org.codehaus.janino:janino:2.5.16")
                implementation("ch.qos.logback:logback-core:1.2.3")
                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
            repositories {
                mavenCentral()
            }
        """.stripIndent())

        def prj2dir = createSubProject('prjCartridge_dev', """
            plugins {
                `java`
                id("com.intershop.icm.cartridge.development")
            }
            repositories {
                mavenCentral()
            }
        """.stripIndent())

        def prj3dir = createSubProject('prjCartridge_adapter', """
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

        writeJavaTestClass("com.intershop.prod",    prj0dir)
        writeJavaTestClass("com.intershop.test",    prj1dir)
        writeJavaTestClass("com.intershop.dev",     prj2dir)
        writeJavaTestClass("com.intershop.adapter", prj3dir)
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    protected def createLocalFile(String path, String content) {
        def testFile = new File(testProjectDir, path)
        testFile.parentFile.mkdirs()
        testFile.text = content.stripIndent()
    }

    protected static void copyDirectory(File source, File target) {
        def sourceRoot = source.toPath()
        def targetRoot = target.toPath()

        Files.walk(sourceRoot).withCloseable { stream ->
            stream.forEach { currentPath ->
                def relativePath = sourceRoot.relativize(currentPath)
                def destinationPath = targetRoot.resolve(relativePath)
                if (Files.isDirectory(currentPath)) {
                    Files.createDirectories(destinationPath)
                } else {
                    Files.copy(currentPath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
