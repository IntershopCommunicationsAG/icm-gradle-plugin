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

package com.intershop.gradle.icm.tasks

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.query.ArtifactResolutionQuery
import org.gradle.api.artifacts.result.ArtifactResolutionResult
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Object class for handling of remote cartridge artifacts.
 */
object CartridgeUtil {

    /**
     * Returns true, if dependency is a external cartridges.
     *
     * @param project   the root project
     * @param moduleID  module ID of the external cartridge
     */
    fun isCartridge(project: Project, moduleID : ModuleComponentIdentifier) : Boolean {
        val query: ArtifactResolutionQuery = project.dependencies.createArtifactResolutionQuery()
            .forModule(moduleID.group, moduleID.module, moduleID.version)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)

        val result: ArtifactResolutionResult = query.execute()

        result.resolvedComponents.forEach { component ->
            val mavenPomArtifacts: Set<ArtifactResult> = component.getArtifacts(MavenPomArtifact::class.java)
            val pomArtifact = mavenPomArtifacts.find {
                it is ResolvedArtifactResult &&it.file.name == "${moduleID.module}-${moduleID.version}.pom"}

            if(pomArtifact != null) {
                val modulePomArtifact = pomArtifact as ResolvedArtifactResult

                try {
                    val doc = readXML(modulePomArtifact.file)
                    val xpFactory = XPathFactory.newInstance()
                    val xPath = xpFactory.newXPath()

                    val xpath = "/project/properties/cartridge.name"
                    val itemsTypeT1 = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

                    return itemsTypeT1.length > 0
                } catch (ex: Exception) {
                    project.logger.info("Pom file is not readable - " + moduleID.moduleIdentifier)
                }
            }
        }
        return false
    }

    private fun readXML(xmlFile: File): Document {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(xmlFile.readText()))

        return dBuilder.parse(xmlInput)
    }

    fun downloadLibFilter(project: Project, dependency: String, name: String): File? {
        val dependencyHandler = project.dependencies
        val dep = dependencyHandler.create(dependency) as ExternalModuleDependency
        dep.artifact {
            it.name = dep.name
            it.classifier = "libs"
            it.extension = "txt"
            it.type = "txt"
        }

        val configuration = project.configurations.maybeCreate(getConfigurationName(project.name, name))
        configuration.setVisible(false)
            .setTransitive(false)
            .setDescription("Libs for download: $name")
            .defaultDependencies { ds ->
                ds.add(dep)
            }

        try {
            val files = configuration.resolve()
            return files.first()
        } catch (anfe: DefaultLenientConfiguration.ArtifactResolveException) {
            project.logger.warn("No library filter is available!")
        }
        return null
    }

    fun getConfigurationName(projectName: String, name: String): String {
        return "${projectName.toLowerCase()}_${name.toLowerCase()}_config"
    }
}
